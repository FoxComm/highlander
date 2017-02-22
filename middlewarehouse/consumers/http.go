package consumers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"log"
	"net/http"
)

func Get(url string, headers map[string]string) (*http.Response, error) {
	return request("GET", url, headers, nil)
}

func Post(url string, headers map[string]string, payload interface{}) (*http.Response, error) {
	return request("POST", url, headers, payload)
}

func Patch(url string, headers map[string]string, payload interface{}) (*http.Response, error) {
	return request("PATCH", url, headers, payload)
}

func request(method string, url string, headers map[string]string, payload interface{}) (*http.Response, error) {
	if method != "POST" && method != "PATCH" && method != "GET" {
		return nil, fmt.Errorf("Invalid method %s. Only GET, POST and PATCH are currently supported", method)
	}

	var req *http.Request
	var err error
	if method == "GET" {
		req, err = http.NewRequest(method, url, nil)
	} else {
		payloadBytes := []byte{}
		payloadBytes, err = json.Marshal(&payload)
		if err != nil {
			return nil, fmt.Errorf("Unable to marshal payload: %s", err.Error())
		}

		log.Printf("HTTP --> %s %s %s", method, url, utils.SanitizePassword(payloadBytes))

		req, err = http.NewRequest(method, url, bytes.NewBuffer(payloadBytes))
	}

	if err != nil {
		return nil, fmt.Errorf("Unable to create %s request: %s", method, err.Error())
	}

	req.Header.Set("Content-Type", "application/json")
	for k, v := range headers {
		req.Header.Set(k, v)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("Unable to make %s request: %s", method, err.Error())
	}

	log.Printf("HTTP <-- %s %s %s", method, url, resp.Status)

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		respBody := new(map[string]interface{})
		defer resp.Body.Close()
		if err := json.NewDecoder(resp.Body).Decode(respBody); err != nil {
			return nil, fmt.Errorf("Error in %s response: %s", method, resp.Status)
		}

		return nil, fmt.Errorf(
			"Error in %s response - status: %s, body %v",
			method,
			resp.Status,
			respBody,
		)
	}

	return resp, nil
}
