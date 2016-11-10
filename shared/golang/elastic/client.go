package elastic

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"

	"github.com/FoxComm/highlander/shared/golang/http"
)

type Client struct {
	url string
}

func NewClient(host, index, mapping string) (*Client, error) {
	if host == "" {
		return nil, errors.New("Host must not be empty")
	} else if index == "" {
		return nil, errors.New("Index must not be empty")
	} else if mapping == "" {
		return nil, errors.New("Mapping must not be empty")
	}

	url := fmt.Sprintf("%s/%s/%s", host, index, mapping)
	return &Client{url}, nil
}

func (c Client) ExecuteSearch(query CompiledQuery) (*Result, error) {
	headers := map[string]string{}
	url := c.searchURL()

	resp, err := http.Post(url, headers, &query)
	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()
	if resp.StatusCode < 200 && resp.StatusCode > 299 {
		return nil, errors.New("Error querying ElasticSearch")
	}

	bytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	result := new(Result)
	if err := json.Unmarshal(bytes, result); err != nil {
		return nil, err
	}

	return result, nil
}

func (c Client) UpdateDocument(id string, payload interface{}) error {
	url := fmt.Sprintf("%s/%s", c.url, id)
	headers := map[string]string{}
	_, err := http.Put(url, headers, payload)
	return err
}

func (c Client) searchURL() string {
	return fmt.Sprintf("%s/_search", c.url)
}
