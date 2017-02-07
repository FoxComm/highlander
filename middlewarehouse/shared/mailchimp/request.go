package mailchimp

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
)

type ErrorResponse struct {
	Type     string `json:"type"`
	Title    string `json:"title"`
	Status   int    `json:"status"`
	Detail   string `json:"detail"`
	Instance string `json:"instance"`
}

func (c *ChimpClient) get(url string, retval interface{}) error {
	return c.request("GET", url, nil, retval)
}

func (c *ChimpClient) post(url string, payload interface{}, retval interface{}) error {
	return c.request("POST", url, payload, retval)
}

func (c *ChimpClient) patch(url string, payload interface{}, retval interface{}) error {
	return c.request("PATCH", url, payload, retval)
}

func (c *ChimpClient) delete(url string) error {
	return c.request("DELETE", url, nil, nil)
}

func (c *ChimpClient) request(method, path string, payload interface{}, retval interface{}) error {
	headers := map[string]string{
		"Content-Type":  "application/json",
		"Authorization": "Basic " + basicAuth("", c.key),
	}

	return request(method, c.apiUrl+path, headers, payload, retval, c.debug)
}

func request(method, url string, headers map[string]string, payload interface{}, retval interface{}, debug bool) error {
	var req *http.Request
	var err error
	payloadBytes := []byte{}

	if method == "GET" {
		req, err = http.NewRequest(method, url, nil)
	} else {
		payloadBytes, err = json.Marshal(&payload)
		if err != nil {
			return fmt.Errorf("Unable to marshal payload: %s", err.Error())
		}

		req, err = http.NewRequest(method, url, bytes.NewBuffer(payloadBytes))
	}

	if debug {
		log.Printf("HTTP --> %s %s %s", method, url, payloadBytes)
	}

	if err != nil {
		return fmt.Errorf("Unable to create %s request: %s", method, err.Error())
	}

	for k, v := range headers {
		req.Header.Set(k, v)
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("Unable to make %s request: %s", method, err.Error())
	}

	if debug {
		log.Printf("HTTP <-- %s %s %s", method, url, resp.Status)
	}

	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return err
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return fmt.Errorf(string(body))
	}

	if retval != nil {
		return json.Unmarshal(body, retval)
	}

	return nil
}

func basicAuth(username, password string) string {
	auth := username + ":" + password

	return base64.StdEncoding.EncodeToString([]byte(auth))
}
