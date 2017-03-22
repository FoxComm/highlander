package elastic

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"

	"github.com/FoxComm/highlander/lib/gohttp"
)

const (
	esIndex      = "%s/%s"
	esMapping    = "%s/%s/_mapping/%s"
	esGetIndices = "%s/_cat/indices?h=index"
)

// Client is our interface the FoxCommerce conventions of using ElasticSearch.
// It handles creating and updating mappings, triggering reindexes, and
// provides status into the current state of the cluster. It does all of this
// with an understanding of how scope affects our indices.
type Client struct {
	hostname string
	indices  []string
}

// NewClient creates a new instance of the Client.
func NewClient(hostname string) *Client {
	return &Client{hostname: hostname}
}

// Connect tests making an HTTP connection to ElasticSearch and populates the
// list of current indices.
func (c *Client) Connect() (err error) {
	c.indices, err = c.getIndices()
	return
}

// GetMappingsInCluster grabs the mapping details from every index in the
// ElasticSearch cluster. The method assumes that mappings with identical names
// in two indices are the same.
func (c *Client) GetMappingsInCluster() (Mappings, error) {
	mappings := Mappings{}

	for _, index := range c.indices {
		trimmedIdx := strings.Trim(index, " ")
		idxMappings, err := c.GetMappingsInIndex(trimmedIdx)
		if err != nil {
			return nil, err
		}

		for name, mapping := range idxMappings {
			mappings[name] = mapping
		}
	}

	return mappings, nil
}

// GetMappingsInIndex pulls the list of all mappings from a single index.
func (c *Client) GetMappingsInIndex(index string) (Mappings, error) {
	url := fmt.Sprintf(esIndex, c.hostname, index)
	var respBody map[string]IndexDetails

	if err := gohttp.Get(url, nil, &respBody); err != nil {
		return nil, err
	}

	details, found := respBody[index]
	if !found {
		return nil, fmt.Errorf("Unable to find details for index %s in response", index)
	}

	return details.Mappings, nil
}

// GetMapping connects to an index and grabs the matching index.
func (c *Client) GetMapping(index, mappingName string) (*Mapping, error) {
	url := fmt.Sprintf(esMapping, c.hostname, index, mappingName)
	var mapping *Mapping

	if err := gohttp.Get(url, nil, mapping); err != nil {
		return nil, err
	}

	return mapping, nil
}

// CreateMapping tests to see whether the mapping exists in the scope, then
// tries to create it. Will error if the mapping already exists.
func (c *Client) CreateMapping(index string, isScoped bool, mappingName string, mappingContents []byte) error {
	return c.createMapping(index, isScoped, mappingName, mappingContents, true)
}

// UpdateMapping tries to update an existing mapping. If this version of the
// mapping has already been pushed, the function acts as a noop.
func (c *Client) UpdateMapping(index string, isScoped bool, mappingName string, mappingContents []byte) error {
	return c.createMapping(index, isScoped, mappingName, mappingContents, false)
}

func (c *Client) createMapping(baseIndex string, isScoped bool, mappingName string, mappingContents []byte, errorIfExists bool) error {
	indexList, err := c.getIndexList(baseIndex, isScoped)
	if err != nil {
		return err
	}

	// Check the status of all mappings before pushing.
	for _, index := range indexList {
		if exists, err := c.testMapping(index, mappingName); err != nil {
			return err
		} else if !exists && errorIfExists {
			return fmt.Errorf("Mapping %s exists in index %s", mappingName, index)
		} else if !exists && !errorIfExists {
			return nil
		}
	}

	// Put the mappings that don't exist
	for _, index := range indexList {
		if err := c.putMapping(index, mappingName, mappingContents); err != nil {
			return err
		}
	}

	return nil
}

func (c *Client) getIndices() ([]string, error) {
	resp, err := http.Get(fmt.Sprintf(esGetIndices, c.hostname))
	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	rawBody := strings.Trim(string(body), "\n\t\r ")
	indices := strings.Split(rawBody, "\n")

	for i, index := range indices {
		indices[i] = strings.Trim(index, " ")
	}

	return indices, nil
}

func (c *Client) getIndexList(indexName string, isScoped bool) ([]string, error) {
	if !isScoped {
		return []string{indexName}, nil
	}

	indexList := []string{}
	for _, index := range c.indices {
		if strings.HasPrefix(index, indexName) {
			indexList = append(indexList, index)
		}
	}

	if len(indexList) == 0 {
		return indexList, fmt.Errorf("No scoped indices found for %s", indexName)
	}

	return indexList, nil
}

func (c *Client) putMapping(index string, mapping string, contents []byte) error {
	url := fmt.Sprintf(esMapping, c.hostname, index, mapping)
	payload := bytes.NewReader(contents)
	respBody := map[string]interface{}{}
	return gohttp.Put(url, nil, payload, &respBody)
}

func (c *Client) testMapping(index, mapping string) (bool, error) {
	url := fmt.Sprintf(esMapping, c.hostname, index, mapping)
	resp, err := http.Get(url)
	if err != nil {
		return false, err
	}

	mappingContent := map[string]interface{}{}
	defer resp.Body.Close()

	if err := json.NewDecoder(resp.Body).Decode(&mappingContent); err != nil {
		return false, err
	}

	if len(mappingContent) == 0 {
		return false, nil
	}

	return true, nil
}
