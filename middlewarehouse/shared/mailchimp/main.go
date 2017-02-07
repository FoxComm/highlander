package mailchimp

import (
	//"bytes"
	//"encoding/json"
	"fmt"
	//"io/ioutil"
	//"log"
	//"net/http"
	"net/url"
	"regexp"
	"time"
)

const (
	ChimpURI       = "%s.api.mailchimp.com"
	ChimpVersion   = "3.0"
	ChimpDCPattern = "[c-z]+[0-9]+$"
	DefaultTimeout = 0
	DefaultDebug   = false
)

type ChimpClient struct {
	key     string
	timeout time.Duration
	apiUrl  string
	debug   bool
}

var ChimpDC = regexp.MustCompile(ChimpDCPattern)

type ChimpOptionFunc func(*ChimpClient)

func SetTimeout(t time.Duration) ChimpOptionFunc {
	return func(cc *ChimpClient) {
		cc.timeout = t
	}
}

func SetDebug(debug bool) ChimpOptionFunc {
	return func(cc *ChimpClient) {
		cc.debug = debug
	}
}

func NewClient(apiKey string, options ...ChimpOptionFunc) *ChimpClient {
	u := url.URL{}

	u.Scheme = "https"
	u.Host = fmt.Sprintf(ChimpURI, ChimpDC.FindString(apiKey))
	u.Path = ChimpVersion

	client := &ChimpClient{
		key:     apiKey,
		apiUrl:  u.String(),
		timeout: DefaultTimeout,
		debug:   DefaultDebug,
	}

	// set options to client
	for _, opt := range options {
		opt(client)
	}

	return client
}
