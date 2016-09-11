package consumers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
)

func Post(url string, headers map[string]string, payload interface{}) (*http.Response, error) {
	payloadBytes, err := json.Marshal(&payload)
	if err != nil {
		return nil, fmt.Errorf("Unable to marshal payload: %s", err.Error())
	}

	log.Printf("Payload contents: %s", string(payloadBytes))

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
		respBody := new(map[string]interface{})
		defer resp.Body.Close()
		if err := json.NewDecoder(resp.Body).Decode(respBody); err != nil {
			return nil, fmt.Errorf("Error in POST response: %s", resp.Status)
		}

		return nil, fmt.Errorf(
			"Error in POST response - status: %s, body %v",
			resp.Status,
			respBody,
		)
	}

	return resp, nil
}

func Patch(url string, headers map[string]string, payload interface{}) (*http.Response, error) {
	payloadBytes, err := json.Marshal(&payload)
	if err != nil {
		return nil, fmt.Errorf("Unable to marshal payload: %s", err.Error())
	}

	log.Printf("Payload contents: %s", string(payloadBytes))

	req, err := http.NewRequest("PATCH", url, bytes.NewBuffer(payloadBytes))
	if err != nil {
		return nil, fmt.Errorf("Unable to create PATCH request: %s", err.Error())
	}

	req.Header.Set("Content-Type", "application/json")
	for k, v := range headers {
		req.Header.Set(k, v)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("Unable to make PATCH request: %s", err.Error())
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		respBody := new(map[string]interface{})
		defer resp.Body.Close()
		if err := json.NewDecoder(resp.Body).Decode(respBody); err != nil {
			return nil, fmt.Errorf("Error in PATCH response: %s", resp.Status)
		}

		return nil, fmt.Errorf(
			"Error in PATCH response - status: %s, body %v",
			resp.Status,
			respBody,
		)
	}

	return resp, nil
}
