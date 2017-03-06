package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"strings"
)

const (
	mappingDir = "./mappings"

	esMapping    = "http://%s/%s/_mapping/%s"
	esGetIndices = "http://%s/_cat/indices?h=index"
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

func (c *Client) UpdateMapping(mapping string, isScoped, isAdmin bool) error {
	// 1. Get the latest definition for this mapping.
	// 2. Check to see if that index exists on the cluster.
	// 3. Create the index if it doesn't
	// 4. Connect to PG and pull contents if creating the index.
	latest, err := getLatestMapping(mapping)
	if err != nil {
		return err
	}

	esMapping, err := getMappingNameForES(latest)
	if err != nil {
		return err
	}

	mappingExists, err := c.testMapping("admin_1.2", esMapping)
	if err != nil {
		return err
	}

	if mappingExists {
		log.Printf("Mapping %s is already present in ES", latest)
		return nil
	}

	mapping, err := readMappingFile(latest)
	if err != nil {
		return err
	}

	if err := c.createMapping("admin_1.2", latest, mapping); err != nil {
		return err
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
	return indices, nil
}

func (c *Client) createMapping(index string, mapping string, contents []byte) error {
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

func getMappingSources() ([]string, error) {
	fileHandles, err := ioutil.ReadDir(mappingDir)
	if err != nil {
		return nil, err
	}

	fileNames := make([]string, len(fileHandles))
	for idx, handles := range fileHandles {
		fileNames[idx] = handles.Name()
	}

	return fileNames, nil
}

func getLatestMapping(mapping string) (string, error) {
	indices, err := getMappingSources()
	if err != nil {
		return "", err
	}

	for i := len(indices) - 1; i >= 0; i-- {
		if strings.HasPrefix(indices[i], mapping) {
			return indices[i], nil
		}
	}

	return "", fmt.Errorf("No mapping files found for %s", mapping)
}

func getMappingNameForES(mappingFilename string) (string, error) {
	if !strings.HasSuffix(mappingFilename, ".json") {
		return "", fmt.Errorf("Mapping filename %s must be JSON", mappingFilename)
	}

	// Strip any directories
	pathParts := strings.Split(mappingFilename, "/")
	filename := pathParts[len(pathParts)-1]

	// Strip the file extension
	idx := len(filename) - 5
	stripped := filename[:idx]

	return strings.ToLower(stripped), nil
}

func readMappingFile(filename string) ([]byte, error) {
	fullFile := fmt.Sprintf("%s/%s", mappingDir, filename)
	return ioutil.ReadFile(fullFile)
}
