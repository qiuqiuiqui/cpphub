package ws

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"

	"github.com/cpphub/blog-agent/internal/audit"
	"github.com/cpphub/blog-agent/internal/commands"
	"github.com/cpphub/blog-agent/internal/config"
	"github.com/gorilla/websocket"
)

// Envelope is the wire-format used in both directions.
type Envelope struct {
	Type      string          `json:"type"`
	ID        string          `json:"id,omitempty"`
	Command   string          `json:"command,omitempty"`
	Params    map[string]any  `json:"params,omitempty"`
	Result    json.RawMessage `json:"result,omitempty"`
	Error     string          `json:"error,omitempty"`
	Timestamp int64           `json:"ts,omitempty"`
}

type Client struct {
	cfg   *config.Config
	cmds  *commands.CommandSet
	audit *audit.Audit

	mu      sync.Mutex
	conn    *websocket.Conn
	stopped bool
	stopCh  chan struct{}
}

// New creates an Agent client. The cmds and audit must already be initialized.
func New(cfg *config.Config, cmds *commands.CommandSet, a *audit.Audit) *Client {
	return &Client{cfg: cfg, cmds: cmds, audit: a, stopCh: make(chan struct{})}
}

// Run blocks until Stop() is called or the underlying connection permanently fails.
// Reconnect logic uses exponential backoff capped at 30s.
func (c *Client) Run() error {
	backoff := time.Second
	for {
		select {
		case <-c.stopCh:
			return nil
		default:
		}

		err := c.connectAndServe()
		c.mu.Lock()
		stopped := c.stopped
		c.mu.Unlock()
		if stopped {
			return nil
		}

		if err != nil {
			log.Printf("[ws] disconnected: %v; reconnecting in %s", err, backoff)
		} else {
			log.Printf("[ws] disconnected; reconnecting in %s", backoff)
		}
		select {
		case <-c.stopCh:
			return nil
		case <-time.After(backoff):
		}
		if backoff < 30*time.Second {
			backoff *= 2
			if backoff > 30*time.Second {
				backoff = 30 * time.Second
			}
		}
	}
}

func (c *Client) Stop() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.stopped = true
	close(c.stopCh)
	if c.conn != nil {
		_ = c.conn.Close()
	}
}

// connectAndServe performs one full session: dial, send hello, then handle commands
// until disconnect.
func (c *Client) connectAndServe() error {
	u, err := c.wssURL()
	if err != nil {
		return err
	}
	hdr := http.Header{}
	hdr.Set("Authorization", "Bearer "+c.cfg.JWT)
	hdr.Set("X-Device-Id", c.cfg.DeviceID)

	dialer := *websocket.DefaultDialer
	dialer.HandshakeTimeout = 10 * time.Second

	log.Printf("[ws] connecting to %s ...", u)
	conn, resp, err := dialer.Dial(u, hdr)
	if err != nil {
		if resp != nil {
			return fmt.Errorf("dial: %s (HTTP %d)", err, resp.StatusCode)
		}
		return fmt.Errorf("dial: %w", err)
	}

	c.mu.Lock()
	c.conn = conn
	c.mu.Unlock()
	defer func() {
		c.mu.Lock()
		c.conn = nil
		c.mu.Unlock()
		_ = conn.Close()
	}()
	log.Printf("[ws] connected")

	// Send hello
	hello := Envelope{Type: "HELLO", Timestamp: time.Now().UnixMilli()}
	if err := conn.WriteJSON(hello); err != nil {
		return fmt.Errorf("write hello: %w", err)
	}

	// ping ticker
	pingStop := make(chan struct{})
	go func() {
		t := time.NewTicker(30 * time.Second)
		defer t.Stop()
		for {
			select {
			case <-pingStop:
				return
			case <-t.C:
				_ = conn.WriteControl(websocket.PingMessage, nil, time.Now().Add(5*time.Second))
			}
		}
	}()
	defer close(pingStop)

	conn.SetReadDeadline(time.Now().Add(90 * time.Second))
	conn.SetPongHandler(func(string) error {
		conn.SetReadDeadline(time.Now().Add(90 * time.Second))
		return nil
	})

	for {
		var env Envelope
		if err := conn.ReadJSON(&env); err != nil {
			return fmt.Errorf("read: %w", err)
		}
		switch env.Type {
		case "PING":
			_ = conn.WriteJSON(Envelope{Type: "PONG", Timestamp: time.Now().UnixMilli()})
		case "CMD_REQUEST":
			c.handleCommand(conn, env)
		default:
			log.Printf("[ws] unknown envelope type: %q", env.Type)
		}
	}
}

func (c *Client) handleCommand(conn *websocket.Conn, env Envelope) {
	start := time.Now()
	ic := commands.InvocationContext{
		Ctx:       context.Background(),
		Params:    env.Params,
		Start:     start,
		UserID:    "unknown", // cloud could include this in a future protocol field
		DeviceID:  c.cfg.DeviceID,
		RequestID: env.ID,
	}
	result, err := c.cmds.Invoke(ic, env.Command)
	dur := time.Since(start)

	entry := audit.Entry{
		RequestID:  env.ID,
		DeviceID:   c.cfg.DeviceID,
		Command:    env.Command,
		DurationMs: dur.Milliseconds(),
		Status:     "OK",
	}

	var (
		stdoutB, stderrB int
		raw              json.RawMessage
	)
	if err != nil {
		entry.Status = "FAIL"
		entry.StderrB = len(err.Error())
		resp := Envelope{Type: "CMD_RESPONSE", ID: env.ID, Error: err.Error(), Timestamp: time.Now().UnixMilli()}
		_ = conn.WriteJSON(resp)
	} else {
		raw, _ = json.Marshal(result)
		stdoutB = len(raw)
		resp := Envelope{Type: "CMD_RESPONSE", ID: env.ID, Result: raw, Timestamp: time.Now().UnixMilli()}
		_ = conn.WriteJSON(resp)
	}
	entry.StdoutB = stdoutB
	entry.StderrB = stderrB
	_ = c.audit.Write(entry)
}

// wssURL converts the cloud base URL to the agent WSS endpoint and injects device_id.
func (c *Client) wssURL() (string, error) {
	base := strings.TrimRight(c.cfg.CloudBase, "/")
	u, err := url.Parse(base)
	if err != nil {
		return "", err
	}
	switch u.Scheme {
	case "http":
		u.Scheme = "ws"
	case "https":
		u.Scheme = "wss"
	}
	u.Path = strings.TrimRight(u.Path, "/") + "/api/agent/ws"
	q := u.Query()
	q.Set("deviceId", c.cfg.DeviceID)
	u.RawQuery = q.Encode()
	return u.String(), nil
}

// to avoid unused import warning when building without certain features
var _ = bytes.MinRead
var _ = errors.New
