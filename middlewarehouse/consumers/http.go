package consumers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

func Get(url string, headers map[string]string) (*http.Response, exceptions.IException) {
	return request("GET", url, headers, nil)
}

func Post(url string, headers map[string]string, payload interface{}) (*http.Response, exceptions.IException) {
	return request("POST", url, headers, payload)
}

func Patch(url string, headers map[string]string, payload interface{}) (*http.Response, exceptions.IException) {
	return request("PATCH", url, headers, payload)
}

func request(method string, url string, headers map[string]string, payload interface{}) (*http.Response, exceptions.IException) {
	if method != "POST" && method != "PATCH" && method != "GET" {
		return nil, NewHttpException(fmt.Errorf("Invalid method %s. Only GET, POST and PATCH are currently supported", method))
	}

	var req *http.Request
	var err error
	if method == "GET" {
		req, err = http.NewRequest(method, url, nil)
	} else {
		payloadBytes := []byte{}
		payloadBytes, err = json.Marshal(&payload)
		if err != nil {
			return nil, NewHttpException(fmt.Errorf("Unable to marshal payload: %s", err.Error()))
		}

		req, err = http.NewRequest(method, url, bytes.NewBuffer(payloadBytes))
	}

	if err != nil {
		return nil, NewHttpException(fmt.Errorf("Unable to create %s request: %s", method, err.Error()))
	}

	req.Header.Set("Content-Type", "application/json")
	for k, v := range headers {
		req.Header.Set(k, v)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, NewHttpException(fmt.Errorf("Unable to make %s request: %s", method, err.Error()))
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		respBody := new(map[string]interface{})
		defer resp.Body.Close()
		if err := json.NewDecoder(resp.Body).Decode(respBody); err != nil {
			return nil, NewHttpException(fmt.Errorf("Error in %s response: %s", method, resp.Status))
		}

		return nil, NewHttpException(fmt.Errorf(
			"Error in %s response - status: %s, body %v",
			method,
			resp.Status,
			respBody,
		))
	}

	return resp, nil
}

type httpException struct {
	cls string `json:"type"`
	exceptions.Exception
}

func NewHttpException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return httpException{
		cls:       "http",
		Exception: exceptions.Exception{error},
	}
}
