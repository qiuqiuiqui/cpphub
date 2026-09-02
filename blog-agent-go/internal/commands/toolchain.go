package commands

import (
	"strings"
	"time"
)

// toolchainInspect probes 9 commonly-used C/C++ toolchain components on Windows.
// SECURITY: This is the ONLY command registry entry that ever invokes external processes for
// toolchain detection. Each probe uses a fixed executable name/path + fixed args + timeout + cap.
func toolchainInspect() *CommandSpec {
	common := []ProbeSpec{
		{
			Name:        "cmake",
			Exe:         "cmake",
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "ninja",
			Exe:         "ninja",
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "git",
			Exe:         "git",
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "make",
			Exe:         "make",
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "python",
			Exe:         "python",
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
	}

	windowsExtra := []ProbeSpec{
		{
			Name: "g++",
			Exe:  "g++",
			// Fallback search order matters: MSYS2 first (most common in C++ learning setups)
			Fallback: []string{
				`C:\msys64\mingw64\bin\g++.exe`,
				`C:\msys64\ucrt64\bin\g++.exe`,
				`C:\mingw64\bin\g++.exe`,
				`C:\TDM-GCC-64\bin\g++.exe`,
				`C:\Program Files\mingw-w64\x86_64-8.1.0-posix-seh-rt_v6-rev0\mingw64\bin\g++.exe`,
			},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name: "clang++",
			Exe:  "clang++",
			Fallback: []string{
				`C:\Program Files\LLVM\bin\clang++.exe`,
				`C:\msys64\mingw64\bin\clang++.exe`,
				`C:\msys64\ucrt64\bin\clang++.exe`,
			},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name: "cl.exe",
			Exe:  "cl",
			Fallback: []string{
				// Visual Studio 2022/2019/2017 default install paths under BuildTools.
				`C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\14.40.17.10\bin\Hostx64\x64\cl.exe`,
				`C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.40.17.10\bin\Hostx64\x64\cl.exe`,
				`C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Tools\MSVC\14.29.30133\bin\Hostx64\x64\cl.exe`,
			},
			Args:        []string{"/Bv"},
			Timeout:     8 * time.Second,
			OutputBytes: 256 * 1024,
		},
		{
			Name: "vcpkg",
			Exe:  "vcpkg",
			Fallback: []string{
				`C:\vcpkg\vcpkg.exe`,
				`C:\src\vcpkg\vcpkg.exe`,
				`C:\tools\vcpkg\vcpkg.exe`,
			},
			Args:        []string{"version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
	}

	probes := append([]ProbeSpec{}, common...)
	probes = append(probes, windowsExtra...)

	parsers := map[string]ParseFunc{
		"g++":     parseGCC,
		"clang++": parseClang,
		"cl.exe":  parseMSVC,
		"cmake":   firstNonEmpty,
		"ninja":   firstNonEmpty,
		"git":     firstNonEmpty,
		"make":    firstNonEmpty,
		"python":  firstNonEmpty,
		"vcpkg":   firstNonEmpty,
	}

	return &CommandSpec{
		Name:    "TOOLCHAIN_INSPECT",
		OS:      "windows",
		Probes:  probes,
		Parsers: parsers,
	}
}

// parsers ----------------------------------------------------------------

func firstNonEmpty(b []byte) string {
	return firstNonEmptyLine(b)
}

// parseGCC extracts "g++ (GCC) 13.2.0" style first line.
func parseGCC(b []byte) string {
	for _, line := range bytesSplitLines(b) {
		s := trim(line)
		if strings.HasPrefix(s, "g++ ") || strings.Contains(s, "GCC") {
			return s
		}
	}
	return firstNonEmptyLine(b)
}

// parseClang extracts "clang version X.Y.Z" style.
func parseClang(b []byte) string {
	for _, line := range bytesSplitLines(b) {
		s := trim(line)
		if strings.HasPrefix(s, "clang version") {
			return s
		}
	}
	return firstNonEmptyLine(b)
}

// parseMSVC extracts "Microsoft (R) C/C++ Optimizing Compiler Version 19.xx for x64".
func parseMSVC(b []byte) string {
	for _, line := range bytesSplitLines(b) {
		s := trim(line)
		if strings.Contains(s, "Microsoft") && strings.Contains(s, "Compiler") {
			return s
		}
	}
	return firstNonEmptyLine(b)
}
