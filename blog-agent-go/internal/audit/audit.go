package audit

import (
	"database/sql"
	"fmt"
	"sync"
	"time"

	_ "modernc.org/sqlite"
)

type Audit struct {
	mu sync.Mutex
	db *sql.DB
}

type Entry struct {
	RequestID  string
	UserID     string
	DeviceID   string
	Command    string
	ExitCode   int
	DurationMs int64
	StdoutB    int
	StderrB    int
	Status     string // "OK" / "FAIL" / "TIMEOUT"
}

// Open opens (or creates) the SQLite audit DB at the given path.
func Open(path string) (*Audit, error) {
	dsn := fmt.Sprintf("file:%s?_pragma=journal_mode(WAL)&_pragma=busy_timeout(5000)&_pragma=synchronous(NORMAL)", path)
	db, err := sql.Open("sqlite", dsn)
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(1)
	if err := db.Ping(); err != nil {
		return nil, err
	}
	if _, err := db.Exec(`
		CREATE TABLE IF NOT EXISTS audit_log (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			ts DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
			request_id TEXT,
			user_id TEXT,
			device_id TEXT,
			command TEXT NOT NULL,
			exit_code INTEGER,
			duration_ms INTEGER,
			stdout_bytes INTEGER,
			stderr_bytes INTEGER,
			status TEXT
		);
		CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_log(ts DESC);
		CREATE INDEX IF NOT EXISTS idx_audit_device ON audit_log(device_id, ts DESC);
	`); err != nil {
		return nil, err
	}
	return &Audit{db: db}, nil
}

func (a *Audit) Close() error {
	a.mu.Lock()
	defer a.mu.Unlock()
	return a.db.Close()
}

// Write appends an entry.
func (a *Audit) Write(e Entry) error {
	a.mu.Lock()
	defer a.mu.Unlock()
	_, err := a.db.Exec(`
		INSERT INTO audit_log (request_id, user_id, device_id, command, exit_code, duration_ms, stdout_bytes, stderr_bytes, status, ts)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
	`, e.RequestID, e.UserID, e.DeviceID, e.Command, e.ExitCode, e.DurationMs, e.StdoutB, e.StderrB, e.Status, time.Now().UTC())
	return err
}
