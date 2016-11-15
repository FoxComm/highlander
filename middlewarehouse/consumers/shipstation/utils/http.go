package utils

import (
	"bytes"
	"encoding/json"
	"errors"
	"io/ioutil"
	"net/http"
)

type HTTPClient struct {
	headers map[string]string
}

func NewHTTPClient() *HTTPClient {
	return &HTTPClient{
		headers: map[string]string{},
	}
}

func (c *HTTPClient) SetHeader(key, value string) {
	c.headers[key] = value
}

func (c HTTPClient) Get(url string, resp interface{}) error {
	return c.request("GET", url, nil, resp)
}

func (c HTTPClient) Post(url string, payload interface{}, resp interface{}) error {
	return c.request("POST", url, payload, resp)
}

func (c HTTPClient) Put(url string, payload interface{}, resp interface{}) error {
	return c.request("PUT", url, payload, resp)
}

func (c HTTPClient) Patch(url string, payload interface{}, resp interface{}) error {
	return c.request("PATCH", url, payload, resp)
}

func (c HTTPClient) request(method string, url string, payload interface{}, respBody interface{}) error {
	client := &http.Client{}
	body := new(bytes.Buffer)

	if method != "GET" {
		if err := json.NewEncoder(body).Encode(payload); err != nil {
			return err
		}
	}

	req, err := http.NewRequest(method, url, body)
	if err != nil {
		return err
	}

	for k, v := range c.headers {
		req.Header.Add(k, v)
	}

	resp, err := client.Do(req)
	if err != nil {
		return err
	}

	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {

		errResp, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return err
		}

		return errors.New(string(errResp))
	}

	return json.NewDecoder(resp.Body).Decode(respBody)
}
