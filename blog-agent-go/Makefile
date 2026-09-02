.PHONY: build build-windows run clean tidy

BIN := blog-agent

build:
	go build -o $(BIN) ./cmd/agent

build-windows:
	GOOS=windows GOARCH=amd64 go build -ldflags "-s -w -H windowsgui" -o $(BIN).exe ./cmd/agent

run: build
	./$(BIN) serve

clean:
	rm -f $(BIN) $(BIN).exe

tidy:
	go mod tidy
