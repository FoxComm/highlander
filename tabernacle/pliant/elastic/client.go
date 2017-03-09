package elastic

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"strings"

	"github.com/FoxComm/highlander/lib/gohttp"
)

const (
	mappingDir = "./mappings"

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

	if _, err := gohttp.GetJSON(url, nil, &respBody); err != nil {
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

	if _, err := gohttp.GetJSON(url, nil, mapping); err != nil {
		return nil, err
	}

	return mapping, nil
}

func (c *Client) CreateMapping(index string, mappingName string, isScoped bool) error {
	mu, err := LatestMappingUpdate(mappingDir, mappingName)
	if err != nil {
		return err
	}

	mappingExists, err := c.testMapping(index, mu.ElasticMapping())
	if err != nil {
		return err
	} else if mappingExists {
		return fmt.Errorf("Mapping %s exists in index %s", esMapping, index)
	}

	return c.createMapping(index, esMapping, mu.Contents)
}

func (c *Client) UpdateMapping(mappingName string, index string, isScoped bool) error {
	mu, err := LatestMappingUpdate(mappingDir, mappingName)
	if err != nil {
		return err
	}

	indexList, err := c.getIndexList(index, isScoped)
	if err != nil {
		return err
	}

	for _, idx := range indexList {
		mappingExists, err := c.testMapping(idx, mu.ElasticMapping())
		if err != nil {
			return err
		}

		if mappingExists {
			log.Printf("Mapping %s is already present in index %s", mu.ElasticMapping(), idx)
			return nil
		}
	}

	for _, idx := range indexList {
		if err := c.createMapping(idx, esMapping, mu.Contents); err != nil {
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

func (c *Client) createMapping(index string, mapping string, contents []byte) error {
	url := fmt.Sprintf(esMapping, c.hostname, index, mapping)
	log.Printf("Pushing mapping to %s", url)

	request, err := http.NewRequest("PUT", url, bytes.NewReader(contents))
	if err != nil {
		return err
	}

	client := http.Client{}
	resp, err := client.Do(request)
	if err != nil {
		return err
	}

	if resp.StatusCode > 299 {
		return fmt.Errorf("Unexpected error updating mapping: %d", resp.StatusCode)
	}

	return nil
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
