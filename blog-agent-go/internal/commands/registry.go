package commands

import (
	"context"
	"fmt"
	"runtime"
	"sync"
	"time"

	"github.com/cpphub/blog-agent/internal/exec"
)

// ProbeSpec describes a single tool probe (e.g., "g++ --version").
type ProbeSpec struct {
	Name        string
	Exe         string   // executable name (looked up via LookPath) OR absolute path
	Fallback    []string // additional candidate absolute paths (Windows-only here)
	Args        []string
	Timeout     time.Duration
	OutputBytes int // stdout/stderr cap; default 64KB
	// IgnoreExit, when true, treats any non-zero exit code as success. Use for tools
	// that print a version banner to stdout and return non-zero (e.g. cl.exe /Bv).
	IgnoreExit bool
	// Category is a coarse tool category used by the UI to group results:
	//   "compiler" / "build" / "package" / "vcs" / "runtime"
	Category string
}

// ParseFunc parses the command stdout into a structured version string.
// If nil, the first non-empty line of stdout is returned as-is.
type ParseFunc func(stdout []byte) string

// CommandSpec describes an invokable command that may trigger multiple probes in parallel.
type CommandSpec struct {
	Name    string
	OS      string // "windows", "linux", "darwin", or "" for all
	Probes  []ProbeSpec
	Parsers map[string]ParseFunc // probe name -> parser; default = first line
}

// InvocationContext is the read-only context passed to commands.
type InvocationContext struct {
	Ctx       context.Context
	Params    map[string]any
	Start     time.Time
	UserID    string
	DeviceID  string
	RequestID string
}

// ProbeResult is what a single probe returns. Multiple installations of the same tool
// (e.g. several g++ versions) are listed in Instances.
type ProbeResult struct {
	Name      string     `json:"name"`
	Category  string     `json:"category"` // "compiler" | "build" | "package" | "vcs" | "runtime"
	Available bool       `json:"available"`
	Instances []Instance `json:"instances"`
	// Back-compat fields — set from the first instance when available.
	Version string `json:"version,omitempty"`
	Path    string `json:"path,omitempty"`
	Error   string `json:"error,omitempty"`
}

// Instance is a single installation of a tool (path + version + source label).
type Instance struct {
	Path    string `json:"path"`
	Version string `json:"version,omitempty"`
	Source  string `json:"source,omitempty"` // "PATH" / "absolute" / label of the candidate
}

// CommandResult is the aggregated output of a CommandSpec.
type CommandResult struct {
	Command string         `json:"command"`
	OS      string         `json:"os"`
	Results []ProbeResult  `json:"results"`
	Extras  map[string]any `json:"extras,omitempty"`
}

// Registry holds all known commands. SECURITY: every entry here is a HARD-LIMIT on what the
// agent will ever run. Adding to this list requires deliberate code change + rebuild.
var Registry = newRegistry()

func newRegistry() *CommandSet {
	return &CommandSet{
		specs: map[string]*CommandSpec{
			"TOOLCHAIN_INSPECT": toolchainInspect(),
		},
	}
}

type CommandSet struct {
	mu    sync.RWMutex
	specs map[string]*CommandSpec
}

func (c *CommandSet) get(name string) (*CommandSpec, bool) {
	c.mu.RLock()
	defer c.mu.RUnlock()
	s, ok := c.specs[name]
	return s, ok
}

// Invoke runs a named command. Returns ErrUnknownCommand if not whitelisted.
func (c *CommandSet) Invoke(ic InvocationContext, name string) (*CommandResult, error) {
	spec, ok := c.get(name)
	if !ok {
		return nil, ErrUnknownCommand{Name: name}
	}
	if spec.OS != "" && spec.OS != runtime.GOOS {
		return &CommandResult{
			Command: name,
			OS:      runtime.GOOS,
			Results: nil,
			Extras: map[string]any{
				"message": fmt.Sprintf("command only supported on %s (current: %s)", spec.OS, runtime.GOOS),
			},
		}, nil
	}

	results := make([]ProbeResult, len(spec.Probes))
	var wg sync.WaitGroup
	for i := range spec.Probes {
		i := i
		p := spec.Probes[i]
		wg.Add(1)
		go func() {
			defer wg.Done()
			results[i] = runProbe(ic, p, spec.Parsers[p.Name])
		}()
	}
	wg.Wait()

	return &CommandResult{
		Command: name,
		OS:      runtime.GOOS,
		Results: results,
	}, nil
}

func runProbe(ic InvocationContext, p ProbeSpec, parser ParseFunc) ProbeResult {
	timeout := p.Timeout
	if timeout <= 0 {
		timeout = 5 * time.Second
	}
	maxBytes := p.OutputBytes
	if maxBytes <= 0 {
		maxBytes = 64 * 1024
	}

	// Collect candidate paths: PATH-resolvable Exe first, then fallbacks.
	type cand struct {
		path   string
		source string
	}
	candidates := []cand{}
	if p.Exe != "" {
		if abs, err := exec.Resolve(p.Exe); err == nil && abs != "" {
			src := "absolute"
			if abs != p.Exe {
				src = "PATH" // looked up via PATH
			}
			candidates = append(candidates, cand{abs, src})
		}
	}
	for _, fb := range p.Fallback {
		if fb == "" {
			continue
		}
		candidates = append(candidates, cand{fb, "fallback"})
	}

	var instances []Instance
	var lastErr error
	seen := map[string]bool{} // de-duplicate by absolute path
	for _, c := range candidates {
		if seen[c.path] {
			continue
		}
		seen[c.path] = true
		out, runErr := exec.RunCaptured(ic.Ctx, c.path, p.Args, timeout, maxBytes)
		if runErr != nil {
			// hard failure (binary not found, fork/exec error) → cannot run this candidate
			if len(out) == 0 {
				lastErr = runErr
				continue
			}
			// soft failure: process ran but exited non-zero. Honor IgnoreExit and/or
			// fall through to parse stdout (banner-style tools).
			if !p.IgnoreExit {
				lastErr = runErr
				continue
			}
		}
		if len(out) == 0 {
			// process ran but produced no output — treat as failure for this candidate
			lastErr = fmt.Errorf("no output")
			continue
		}
		version := ""
		if parser != nil {
			version = parser(out)
		} else {
			version = firstNonEmptyLine(out)
		}
		instances = append(instances, Instance{
			Path:    c.path,
			Version: version,
			Source:  c.source,
		})
	}

	if len(instances) == 0 {
		msg := "not found"
		if lastErr != nil {
			msg = lastErr.Error()
		}
		return ProbeResult{
			Name:      p.Name,
			Category:  p.Category,
			Available: false,
			Error:     msg,
		}
	}
	res := ProbeResult{
		Name:      p.Name,
		Category:  p.Category,
		Available: true,
		Instances: instances,
	}
	// Back-compat: first instance is the "primary".
	res.Version = instances[0].Version
	res.Path = instances[0].Path
	return res
}

func firstNonEmptyLine(b []byte) string {
	for _, line := range bytesSplitLines(b) {
		s := trim(line)
		if s != "" {
			return s
		}
	}
	return ""
}

// minimal helpers (avoids pulling strings/bytes for a tiny util)
func bytesSplitLines(b []byte) [][]byte {
	var lines [][]byte
	start := 0
	for i, c := range b {
		if c == '\n' {
			lines = append(lines, b[start:i])
			start = i + 1
		}
	}
	if start < len(b) {
		lines = append(lines, b[start:])
	}
	return lines
}

func trim(b []byte) string {
	s := string(b)
	// trim whitespace + CR
	for len(s) > 0 && (s[len(s)-1] == '\r' || s[len(s)-1] == ' ' || s[len(s)-1] == '\t') {
		s = s[:len(s)-1]
	}
	for len(s) > 0 && (s[0] == ' ' || s[0] == '\t') {
		s = s[1:]
	}
	return s
}

// NewInvocationContext builds an InvocationContext for CLI testing.
func NewInvocationContext(params map[string]any, start time.Time, userID, deviceID, requestID string) InvocationContext {
	return InvocationContext{
		Ctx:       context.Background(),
		Params:    params,
		Start:     start,
		UserID:    userID,
		DeviceID:  deviceID,
		RequestID: requestID,
	}
}

// ErrUnknownCommand is returned when a non-whitelisted command is invoked.
type ErrUnknownCommand struct{ Name string }

func (e ErrUnknownCommand) Error() string { return "unknown command: " + e.Name }
