package pairing

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

// Pair exchanges a one-time pairing code for a deviceId + JWT.
func Pair(cloudBase, code string) (deviceID, jwt string, err error) {
	base := strings.TrimRight(cloudBase, "/")
	url := base + "/api/agent/pair"
	body, _ := json.Marshal(map[string]string{"code": code})
	req, err := http.NewRequest("POST", url, bytes.NewReader(body))
	if err != nil {
		return "", "", err
	}
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 15 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return "", "", fmt.Errorf("network: %w", err)
	}
	defer resp.Body.Close()

	bodyBytes, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != 200 {
		return "", "", fmt.Errorf("server returned %d: %s", resp.StatusCode, string(bodyBytes))
	}

	var out struct {
		Code    int    `json:"code"`
		Message string `json:"message"`
		Data    struct {
			DeviceID string `json:"deviceId"`
			JWT      string `json:"jwt"`
		} `json:"data"`
	}
	if err := json.Unmarshal(bodyBytes, &out); err != nil {
		return "", "", fmt.Errorf("decode: %w", err)
	}
	if out.Code != 0 {
		return "", "", fmt.Errorf("pair refused: %s", out.Message)
	}
	if out.Data.DeviceID == "" || out.Data.JWT == "" {
		return "", "", fmt.Errorf("incomplete response: %s", string(bodyBytes))
	}
	return out.Data.DeviceID, out.Data.JWT, nil
}
