package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/cpphub/blog-agent/internal/audit"
	"github.com/cpphub/blog-agent/internal/commands"
	"github.com/cpphub/blog-agent/internal/config"
	"github.com/cpphub/blog-agent/internal/pairing"
	"github.com/cpphub/blog-agent/internal/ws"
)

const version = "0.1.0"

func main() {
	if len(os.Args) < 2 {
		printUsage()
		os.Exit(1)
	}

	subcmd := os.Args[1]
	switch subcmd {
	case "version", "-v", "--version":
		fmt.Printf("blog-agent %s\n", version)
	case "pair":
		runPair()
	case "serve":
		runServe()
	case "status":
		runStatus()
	case "inspect":
		runInspect()
	default:
		fmt.Fprintf(os.Stderr, "unknown subcommand: %s\n", subcmd)
		printUsage()
		os.Exit(1)
	}
}

func printUsage() {
	fmt.Fprintln(os.Stderr, `blog-agent - C++ Hub local agent

Usage:
  blog-agent pair --code <6-digit-code>   Pair with cloud backend using a one-time code
  blog-agent serve                        Run WSS client (foreground)
  blog-agent status                       Show current device status
  blog-agent inspect                      Run a one-shot local toolchain inspection
  blog-agent version                      Print version`)
}

func runPair() {
	var code string
	for i := 2; i < len(os.Args)-1; i++ {
		if os.Args[i] == "--code" || os.Args[i] == "-c" {
			code = os.Args[i+1]
			break
		}
	}
	if code == "" {
		fmt.Fprintln(os.Stderr, "missing --code <6-digit-code>")
		os.Exit(2)
	}

	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintln(os.Stderr, "load config:", err)
		os.Exit(1)
	}

	fmt.Printf("Pairing with %s ...\n", cfg.CloudBase)
	deviceID, jwt, err := pairing.Pair(cfg.CloudBase, code)
	if err != nil {
		fmt.Fprintln(os.Stderr, "pair failed:", err)
		os.Exit(1)
	}

	cfg.DeviceID = deviceID
	cfg.JWT = jwt
	if err := config.Save(cfg); err != nil {
		fmt.Fprintln(os.Stderr, "save config:", err)
		os.Exit(1)
	}
	fmt.Println("Paired successfully!")
	fmt.Println("  device_id:", deviceID)
	fmt.Println("Now run: blog-agent serve")
}

func runServe() {
	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintln(os.Stderr, "load config:", err)
		os.Exit(1)
	}
	if cfg.JWT == "" || cfg.DeviceID == "" {
		fmt.Fprintln(os.Stderr, "agent not paired. Run: blog-agent pair --code <code>")
		os.Exit(1)
	}

	// open audit DB
	dbPath := filepath.Join(cfg.DataDir, "audit.db")
	a, err := audit.Open(dbPath)
	if err != nil {
		fmt.Fprintln(os.Stderr, "open audit db:", err)
		os.Exit(1)
	}
	defer a.Close()

	client := ws.New(cfg, commands.Registry, a)
	fmt.Printf("blog-agent %s serving as device %s\n", version, cfg.DeviceID)
	fmt.Printf("connected to %s\n", cfg.CloudBase)

	if err := client.Run(); err != nil {
		fmt.Fprintln(os.Stderr, "agent exited:", err)
		os.Exit(1)
	}
}

func runStatus() {
	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintln(os.Stderr, "load config:", err)
		os.Exit(1)
	}
	if cfg.DeviceID == "" {
		fmt.Println("not paired")
		return
	}
	fmt.Println("paired")
	fmt.Println("  device_id:", cfg.DeviceID)
	fmt.Println("  cloud:    ", cfg.CloudBase)
	fmt.Println("  config:   ", config.Path())
	fmt.Println("  data_dir: ", cfg.DataDir)
}

func runInspect() {
	// Run a one-shot toolchain inspection and print pretty JSON.
	// Used for quick CLI testing without going through the cloud.
	ctx := commands.NewInvocationContext(map[string]any{}, time.Now(), "local-cli", "local-cli", "")
	result, err := commands.Registry.Invoke(ctx, "TOOLCHAIN_INSPECT")
	if err != nil {
		fmt.Fprintln(os.Stderr, "inspect error:", err)
		os.Exit(1)
	}
	b, _ := json.MarshalIndent(result, "", "  ")
	fmt.Println(string(b))

	// prompt for "press enter to exit" so the cmd window doesn't close instantly on double-click
	if isInteractive() {
		fmt.Fprintln(os.Stderr, "\n(press Enter to exit)")
		bufio.NewReader(os.Stdin).ReadString('\n')
	}
}

func isInteractive() bool {
	fi, err := os.Stdin.Stat()
	if err != nil {
		return false
	}
	return (fi.Mode()&os.ModeCharDevice) != 0 && !strings.HasSuffix(os.Args[0], ".exe")
}
