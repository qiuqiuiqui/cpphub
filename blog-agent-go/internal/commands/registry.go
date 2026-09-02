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

// ProbeResult is what a single probe returns.
type ProbeResult struct {
	Name      string `json:"name"`
	Available bool   `json:"available"`
	Version   string `json:"version,omitempty"`
	Path      string `json:"path,omitempty"`
	Error     string `json:"error,omitempty"`
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

	candidates := []string{p.Exe}
	candidates = append(candidates, p.Fallback...)

	var lastErr error
	for _, exe := range candidates {
		if exe == "" {
			continue
		}
		out, err := exec.RunCaptured(ic.Ctx, exe, p.Args, timeout, maxBytes)
		if err == nil {
			version := ""
			if parser != nil {
				version = parser(out)
			} else {
				version = firstNonEmptyLine(out)
			}
			abs, _ := exec.Resolve(exe)
			return ProbeResult{
				Name:      p.Name,
				Available: true,
				Version:   version,
				Path:      abs,
			}
		}
		lastErr = err
	}

	msg := "not found"
	if lastErr != nil {
		msg = lastErr.Error()
	}
	return ProbeResult{
		Name:      p.Name,
		Available: false,
		Error:     msg,
	}
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
