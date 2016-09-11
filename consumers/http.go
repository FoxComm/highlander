package consumers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
)

func Post(url string, headers map[string]string, payload interface{}) (*http.Response, error) {
	payloadBytes, err := json.Marshal(&payload)
	if err != nil {
		return nil, fmt.Errorf("Unable to marshal payload: %s", err.Error())
	}

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(payloadBytes))
	if err != nil {
		return nil, fmt.Errorf("Unable to create POST request: %s", err.Error())
	}

	req.Header.Set("Content-Type", "application/json")
	for k, v := range headers {
		req.Header.Set(k, v)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("Unable to make POST request: %s", err.Error())
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return resp, fmt.Errorf("Error in POST response: %s", resp.Status)
	}

	return resp, nil
}
