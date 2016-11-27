package utils

import (
	"bytes"
	"encoding/json"
	"errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers"
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

func (c HTTPClient) Get(url string, resp interface{}) exceptions.IException {
	return c.request("GET", url, nil, resp)
}

func (c HTTPClient) Post(url string, payload interface{}, resp interface{}) exceptions.IException {
	return c.request("POST", url, payload, resp)
}

func (c HTTPClient) Put(url string, payload interface{}, resp interface{}) exceptions.IException {
	return c.request("PUT", url, payload, resp)
}

func (c HTTPClient) Patch(url string, payload interface{}, resp interface{}) exceptions.IException {
	return c.request("PATCH", url, payload, resp)
}

func (c HTTPClient) request(method string, url string, payload interface{}, respBody interface{}) exceptions.IException {
	client := &http.Client{}
	body := new(bytes.Buffer)

	if method != "GET" {
		if err := json.NewEncoder(body).Encode(payload); err != nil {
			return consumers.NewHttpException(err)
		}
	}

	req, err := http.NewRequest(method, url, body)
	if err != nil {
		return consumers.NewHttpException(err)
	}

	for k, v := range c.headers {
		req.Header.Add(k, v)
	}

	resp, err := client.Do(req)
	if err != nil {
		return consumers.NewHttpException(err)
	}

	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {

		errResp, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return consumers.NewHttpException(err)
		}

		return consumers.NewHttpException(errors.New(string(errResp)))
	}

	return consumers.NewHttpException(json.NewDecoder(resp.Body).Decode(respBody))
}
