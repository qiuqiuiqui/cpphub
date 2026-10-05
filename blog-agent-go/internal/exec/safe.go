package exec

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"os/exec"
	"path/filepath"
	"syscall"
	"time"
)

// MaxOutputBytes is the hard upper bound on stdout/stderr we will ever capture.
const MaxOutputBytes = 1024 * 1024 // 1 MB

// RunCaptured executes the given command with absolute args (no shell), applies a timeout,
// and caps the captured output. Returns stdout (truncated to maxBytes).
//
// SECURITY: never pass user-controlled strings as the cmd name; always go through the
// registry in commands/. Args must be a pre-validated []string.
//
// Returns:
//   - (stdout, nil) on success (exit 0)
//   - (stdout, *exec.ExitError) when the process ran but exited non-zero — caller can
//     still use stdout if it carries useful info (e.g. version banners)
//   - (nil, wrappedPathError) when fork/exec itself failed (binary missing, ACL, etc.)
func RunCaptured(ctx context.Context, name string, args []string, timeout time.Duration, maxBytes int) ([]byte, error) {
	if name == "" {
		return nil, errors.New("empty command name")
	}
	if maxBytes <= 0 || maxBytes > MaxOutputBytes {
		maxBytes = MaxOutputBytes
	}

	cctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	cmd := exec.CommandContext(cctx, name, args...)
	// HideWindow prevents a console window from flashing on Windows when the agent
	// is launched as a GUI subsystem binary.
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}

	var stdout, stderr bytes.Buffer
	stdout.Grow(maxBytes)
	stderr.Grow(maxBytes)
	limitedOut := &limitedWriter{w: &stdout, n: maxBytes}
	limitedErr := &limitedWriter{w: &stderr, n: maxBytes}
	cmd.Stdout = limitedOut
	cmd.Stderr = limitedErr

	err := cmd.Run()
	// timeout context canceled -> report deadline exceeded
	if cctx.Err() == context.DeadlineExceeded {
		// Even on timeout, return whatever was captured so far (stdout || stderr).
		return mergeForOutput(stdout.Bytes(), stderr.Bytes()), fmt.Errorf("timeout after %s", timeout)
	}
	if err != nil {
		// exec.ExitError = process ran but exited non-zero (still useful: stdout may carry banner).
		// Anything else (e.g. ENOENT, EACCES, fork/exec failure, missing DLL) is a hard start failure.
		var exitErr *exec.ExitError
		if errors.As(err, &exitErr) {
			// Combine stdout + stderr so callers see all output (some tools like cl.exe write
			// version info to stderr instead of stdout).
			combined := mergeForOutput(stdout.Bytes(), stderr.Bytes())
			msg := err.Error()
			if e := stderr.Bytes(); len(e) > 0 {
				if len(e) > 512 {
					e = e[:512]
				}
				msg += ": " + string(e)
			}
			return combined, &exec.ExitError{
				Stderr: []byte(msg),
			}
		}
		// Hard failure (binary not found OR DLL missing on Windows). Include stderr if any.
		msg := err.Error()
		if e := stderr.Bytes(); len(e) > 0 {
			if len(e) > 256 {
				e = e[:256]
			}
			msg += " (stderr: " + string(e) + ")"
		}
		return nil, errors.New(msg)
	}
	return mergeForOutput(stdout.Bytes(), stderr.Bytes()), nil
}

// mergeForOutput concatenates stdout and stderr, preferring stdout when present.
// Used because some tools (notably MSVC cl.exe with /Bv) write banner/version
// information to stderr instead of stdout.
func mergeForOutput(stdout, stderr []byte) []byte {
	if len(stdout) == 0 {
		return stderr
	}
	if len(stderr) == 0 {
		return stdout
	}
	combined := make([]byte, 0, len(stdout)+len(stderr)+1)
	combined = append(combined, stdout...)
	combined = append(combined, '\n')
	combined = append(combined, stderr...)
	return combined
}

// Resolve returns the absolute path that would be executed for `name`, if locatable.
// Falls back to returning the input name (absolute or not) when LookPath can't find it.
func Resolve(name string) (string, error) {
	if name == "" {
		return "", errors.New("empty name")
	}
	if filepath.IsAbs(name) {
		return name, nil
	}
	if abs, err := exec.LookPath(name); err == nil {
		return abs, nil
	}
	return name, nil
}

// limitedWriter caps the number of bytes written to w. Anything beyond is silently dropped
// (we still update n so callers can see overflow happened).
type limitedWriter struct {
	w       *bytes.Buffer
	n       int
	written int
}

func (l *limitedWriter) Write(p []byte) (int, error) {
	if l.written >= l.n {
		// Still consume but don't store.
		l.written += len(p)
		return len(p), nil
	}
	remaining := l.n - l.written
	if len(p) <= remaining {
		l.written += len(p)
		return l.w.Write(p)
	}
	l.w.Write(p[:remaining])
	l.written = l.n
	return len(p), nil
}
