package commands

import (
	"strings"
	"time"
)

// toolchainInspect probes C/C++ toolchain components on Windows.
//
// SECURITY: This is the ONLY command registry entry that invokes external processes for
// toolchain detection. Each probe uses a fixed list of candidate executables (looked up
// via PATH + well-known absolute paths), fixed args, a hard timeout, and a stdout cap.
//
// All candidates are tried — the agent reports EVERY detected installation, not just the
// first one. This lets the user pick the right version when multiple compilers coexist.
func toolchainInspect() *CommandSpec {
	common := []ProbeSpec{
		// cmake: searched in PATH plus common install roots. We list several absolute
		// paths because projects often ship bundled cmake (e.g. D:\QT\Tools\CMake_64).
		{
			Name:     "cmake",
			Exe:      "cmake",
			Category: "build",
			Fallback: []string{
				`C:\CMake\bin\cmake.exe`,
				`C:\Program Files\CMake\bin\cmake.exe`,
				`D:\CMake\bin\cmake.exe`,
				`D:\QT\Qt6\Tools\CMake_64\bin\cmake.exe`,
				`D:\Matlab\bin\win64\cmake\bin\cmake.exe`,
				`E:\CMake\bin\cmake.exe`,
			},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "ninja",
			Exe:         "ninja",
			Category:    "build",
			Fallback:    []string{`C:\ninja\ninja.exe`, `D:\ninja\ninja.exe`, `D:\QT\Qt6\Tools\Ninja\ninja.exe`},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "git",
			Exe:         "git",
			Category:    "vcs",
			Fallback:    []string{`C:\Program Files\Git\bin\git.exe`, `C:\Program Files (x86)\Git\bin\git.exe`, `D:\Git\cmd\git.exe`},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "make",
			Exe:         "make",
			Category:    "build",
			Fallback:    []string{`C:\MinGW\bin\mingw32-make.exe`, `D:\MinGW\bin\mingw32-make.exe`, `D:\msys64\usr\bin\make.exe`},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:        "python",
			Exe:         "python",
			Category:    "runtime",
			Fallback:    []string{`C:\Python313\python.exe`, `C:\Python312\python.exe`, `C:\Python311\python.exe`, `C:\Python310\python.exe`, `E:\Python\python.exe`, `D:\Python\python.exe`},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
	}

	windowsExtra := []ProbeSpec{
		// g++: many users have MinGW + Strawberry + Qt-bundled MinGW side by side.
		{
			Name:     "g++",
			Exe:      "g++",
			Category: "compiler",
			Fallback: []string{
				// MSYS2 (ucrt64 is the modern default)
				`C:\msys64\ucrt64\bin\g++.exe`,
				`C:\msys64\mingw64\bin\g++.exe`,
				`C:\msys64\clang64\bin\g++.exe`,
				// Standalone MinGW releases
				`C:\mingw64\bin\g++.exe`,
				`C:\TDM-GCC-64\bin\g++.exe`,
				`C:\Program Files\mingw-w64\x86_64-8.1.0-posix-seh-rt_v6-rev0\mingw64\bin\g++.exe`,
				// User-level releases (any drive letter root)
				`D:\mingw64\bin\g++.exe`,
				`D:\x86_64-14.2.0-release-posix-seh-ucrt-rt_v12-rev2\mingw64\bin\g++.exe`,
				// Strawberry Perl ships gcc (often unwanted, but real)
				`C:\Strawberry\c\bin\g++.exe`,
				// Qt bundles MinGW
				`D:\QT\Qt6\Tools\mingw1310_64\bin\g++.exe`,
				`D:\QT\Qt5.15.2\Tools\mingw1310_64\bin\g++.exe`,
			},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		{
			Name:     "clang++",
			Exe:      "clang++",
			Category: "compiler",
			Fallback: []string{
				`C:\Program Files\LLVM\bin\clang++.exe`,
				`C:\Program Files (x86)\LLVM\bin\clang++.exe`,
				`C:\msys64\mingw64\bin\clang++.exe`,
				`C:\msys64\ucrt64\bin\clang++.exe`,
				`C:\msys64\clang64\bin\clang++.exe`,
				`D:\LLVM\bin\clang++.exe`,
			},
			Args:        []string{"--version"},
			Timeout:     5 * time.Second,
			OutputBytes: 64 * 1024,
		},
		// cl.exe (MSVC): multiple Visual Studio installs are common (VS2017 + VS2019 + VS2022).
		// We probe the well-known vcvars layout; if vcvars is needed the user must run from a
		// Developer Command Prompt — but `cl /Bv` itself does NOT need vcvars.
		{
			Name:     "cl.exe",
			Exe:      "cl",
			Category: "compiler",
			// cl.exe /Bv always exits non-zero but prints a version banner.
			Fallback: []string{
				// VS 2022 (default)
				`C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.40.17.10\bin\Hostx64\x64\cl.exe`,
				`C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Tools\MSVC\14.40.17.10\bin\Hostx64\x64\cl.exe`,
				`C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\14.40.17.10\bin\Hostx64\x64\cl.exe`,
				`C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\14.40.17.10\bin\Hostx64\x64\cl.exe`,
				// VS 2019
				`C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Tools\MSVC\14.29.30133\bin\Hostx64\x64\cl.exe`,
				`C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\bin\Hostx64\x64\cl.exe`,
				// D:\VS Studio (this user's setup)
				`D:\VS Studio\VC\Tools\MSVC\14.44.35207\bin\Hostx64\x64\cl.exe`,
				// VS 2017
				`C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools\VC\Tools\MSVC\14.16.27023\bin\Hostx64\x64\cl.exe`,
			},
			Args:        []string{"/Bv"},
			IgnoreExit:  true, // cl.exe /Bv exits non-zero but prints a banner
			Timeout:     8 * time.Second,
			OutputBytes: 256 * 1024,
		},
		{
			Name:     "vcpkg",
			Exe:      "vcpkg",
			Category: "package",
			Fallback: []string{
				`C:\vcpkg\vcpkg.exe`,
				`C:\src\vcpkg\vcpkg.exe`,
				`C:\tools\vcpkg\vcpkg.exe`,
				`D:\vcpkg\vcpkg.exe`,
				`E:\vcpkg\vcpkg.exe`,
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

// parseMSVC extracts the cl.exe version. cl.exe /Bv writes the banner to stderr in
// the current console codepage (often GBK on Chinese Windows), so the human-readable
// banner comes back as mojibake. However the ASCII version number always appears
// somewhere as `Version NN.NN.NNNN` or as `NN.NN.NNNN` next to it. We scan for the
// major 19/18/17/16 marker and emit a clean ASCII string for the UI.
func parseMSVC(b []byte) string {
	for _, line := range bytesSplitLines(b) {
		s := trim(line)
		// The DLL-list line that always follows the localized banner is ASCII-ish:
		//   " D:\VS Studio\...\cl.exe:        <GBK 'Version'>19.44.35226.0"
		// We scan for any of the known major MSVC version prefixes (19/18/17/16) and
		// consume digits/dots after it.
		for _, prefix := range []string{"19.", "18.", "17.", "16."} {
			idx := strings.Index(s, prefix)
			if idx < 0 {
				continue
			}
			// Make sure this isn't a random "19." inside another number (e.g. path).
			// Require that the char before "19." is non-digit (so e.g. "919." doesn't match).
			if idx > 0 {
				prev := s[idx-1]
				if prev >= '0' && prev <= '9' {
					continue
				}
			}
			tail := s[idx:]
			end := 0
			for end < len(tail) {
				c := tail[end]
				if (c >= '0' && c <= '9') || c == '.' {
					end++
					continue
				}
				break
			}
			if end >= 5 { // at least major.minor
				return "Microsoft C/C++ Optimizing Compiler Version " + tail[:end]
			}
		}
	}
	return "Microsoft C/C++ Compiler (version banner localized; see path)"
}
