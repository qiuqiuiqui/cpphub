package config

import (
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
)

type Config struct {
	// CloudBase is the blog backend root, e.g. https://blog.example.com (no trailing slash).
	CloudBase string `json:"cloudBase"`
	// DeviceID returned by the cloud after pairing.
	DeviceID string `json:"deviceId"`
	// JWT used as bearer when connecting WSS.
	JWT string `json:"jwt"`
	// DataDir holds agent.key (future) and audit.db.
	DataDir string `json:"dataDir"`
}

const defaultCloudBase = "http://localhost:8090"

// Path returns the absolute path to config.json.
func Path() string {
	dir, err := os.UserConfigDir()
	if err != nil {
		dir, _ = os.UserHomeDir()
	}
	return filepath.Join(dir, "cpphub-agent", "config.json")
}

func dataDir() string {
	dir, err := os.UserConfigDir()
	if err != nil {
		dir, _ = os.UserHomeDir()
	}
	return filepath.Join(dir, "cpphub-agent")
}

// Load reads config from disk. Returns defaults if not present.
func Load() (*Config, error) {
	path := Path()
	b, err := os.ReadFile(path)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return defaults(), nil
		}
		return nil, err
	}
	var c Config
	if err := json.Unmarshal(b, &c); err != nil {
		return nil, err
	}
	if c.CloudBase == "" {
		c.CloudBase = defaultCloudBase
	}
	if c.DataDir == "" {
		c.DataDir = dataDir()
	}
	return &c, nil
}

func Save(c *Config) error {
	if err := os.MkdirAll(c.DataDir, 0o755); err != nil {
		return err
	}
	if c.CloudBase == "" {
		c.CloudBase = defaultCloudBase
	}
	if c.DataDir == "" {
		c.DataDir = dataDir()
	}
	b, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(Path()), 0o755); err != nil {
		return err
	}
	return os.WriteFile(Path(), b, 0o600)
}

func defaults() *Config {
	return &Config{
		CloudBase: defaultCloudBase,
		DataDir:   dataDir(),
	}
}
