package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type BucketStats struct {
	Agg int64 `json:"agg"`
	Val int64 `json:"val"`
}

type DiffStats struct {
	Variance   int64       `json:"variance"`
	Mean       int64       `json:"mean"`
	Resolution int64       `json:"resolution"`
	Points     int64       `json:"points"`
	Sum        int64       `json:"sum"`
	Right      BucketStats `json:"right"`
	Left       BucketStats `json:"left"`
}

type Diff struct {
	Stats DiffStats `json:"stats"`
	Key   string    `json:"key"`
}

type SummaryStats struct {
	Variance   float64 `json:"variance"`
	Mean       float64 `json:"mean"`
	From       int64   `json:"from"`
	Resolution int64   `json:"resolution"`
	Points     int64   `json:"points"`
	To         int64   `json:"to"`
	Sum        int64   `json:"sum"`
}

type Summary struct {
	Stats SummaryStats `json:"stats"`
	Key   string       `json:"key"`
}

type Henhouse struct {
	httpHost     string
	httpPort     int16
	henhouseConn net.Conn
}

func NewHenhouse(host string, httpPort int16, inputPort int16) (*Henhouse, error) {
	if host == "" {
		return nil, errors.New("http host is required")
	}

	henhouseConn, err := net.Dial("tcp", fmt.Sprintf("%s:%d", host, inputPort))
	if err != nil {
		return nil, fmt.Errorf("Unable to connect to henhouse with error %s", err.Error())
	}

	return &Henhouse{host, httpPort, henhouseConn}, nil
}

func (h *Henhouse) Track(key string, value int64, time time.Time) error {
	message := fmt.Sprintf("%s %d %d\n", key, value, time.Unix())
	var _, err = fmt.Fprintln(h.henhouseConn, message)
	return err
}

func (h *Henhouse) Summary(keys []string) ([]Summary, error) {
	content, err := h.summaryRaw(keys)
	if err != nil {
		return nil, err
	}
	resultArray := make([]Summary, 0)
	json.Unmarshal(content, &resultArray)
	return resultArray, nil
}

func (h *Henhouse) summaryRaw(keys []string) ([]byte, error) {
	keysString := url.QueryEscape(strings.Join(keys, ","))

	query := fmt.Sprintf("http://%s:%d/summary?keys=%s", h.httpHost, h.httpPort, keysString)
	log.Printf("Querying: %s", query)
	req, err := http.NewRequest("GET", query, nil)

	if err != nil {
		return nil, err
	}

	resp, err := http.DefaultClient.Do(req)

	if err != nil {
		return nil, err
	}

	content, err := ioutil.ReadAll(resp.Body)
	resp.Body.Close()

	if resp.StatusCode != 200 {
		log.Fatalf("Cannot query henhouse: %d, %s", resp.StatusCode, string(content))
		return nil, fmt.Errorf("Cannot query henhouse: %d", resp.StatusCode)
	}

	return content, nil
}

func (h *Henhouse) Diff(keys []string, a time.Time, b time.Time) ([]Diff, error) {
	content, err := h.diffRaw(keys, a, b)
	if err != nil {
		return nil, err
	}
	resultArray := make([]Diff, 0)
	json.Unmarshal(content, &resultArray)
	return resultArray, nil
}

func (h *Henhouse) diffRaw(keys []string, a time.Time, b time.Time) ([]byte, error) {
	keysString := url.QueryEscape(strings.Join(keys, ","))

	query := fmt.Sprintf("http://%s:%d/diff?keys=%s&a=%d&b=%d", h.httpHost, h.httpPort, keysString, a.Unix(), b.Unix())
	log.Printf("Querying: %s", query)

	req, err := http.NewRequest("GET", query, nil)

	if err != nil {
		return nil, err
	}

	resp, err := http.DefaultClient.Do(req)

	if err != nil {
		return nil, err
	}

	content, err := ioutil.ReadAll(resp.Body)
	resp.Body.Close()

	if resp.StatusCode != 200 {
		log.Fatalf("Cannot query henhouse: %d, %s", resp.StatusCode, string(content))
		return nil, fmt.Errorf("Cannot query henhouse: %d", resp.StatusCode)
	}

	return content, nil
}
