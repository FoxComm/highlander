package cmd

import (
	"fmt"
	"io/ioutil"
	"strings"
)

type MappingVersion struct {
	Filename string
	Contents []byte
}

func NewMappingVersion(fileDir, filename string) (*MappingVersion, error) {
	path := fmt.Sprintf("%s/%s", fileDir, filename)
	contents, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, err
	}

	return &MappingVersion{
		Filename: filename,
		Contents: contents,
	}, nil
}

func LatestMappingVersion(fileDir, baseName string) (*MappingVersion, error) {
	latestFilename, err := getLatestMapping(fileDir, baseName)
	if err != nil {
		return nil, err
	}

	return NewMappingVersion(fileDir, latestFilename)
}

func NewMappingVersions(fileDir string) (map[string]*MappingVersion, error) {
	fileHandles, err := ioutil.ReadDir(fileDir)
	if err != nil {
		return nil, err
	}

	mappings := map[string]*MappingVersion{}
	for i := len(fileHandles); i >= 0; i-- {
		handle := fileHandles[i].Name()
		baseName, err := extractBaseName(handle)
		if err != nil {
			return nil, err
		}

		if _, found := mappings[baseName]; !found {
			mu, err := NewMappingVersion(fileDir, handle)
			if err != nil {
				return nil, err
			}
			mappings[baseName] = mu
		}
	}
	return mappings, nil
}

func (m *MappingVersion) ElasticMapping() string {
	// Strip any directories
	pathParts := strings.Split(m.Filename, "/")
	filename := pathParts[len(pathParts)-1]

	// Strip the file extension
	idx := len(filename) - 5
	stripped := filename[:idx]

	return strings.ToLower(stripped)
}

func getLatestMapping(fileDir, mapping string) (string, error) {
	fileHandles, err := ioutil.ReadDir(fileDir)
	if err != nil {
		return "", err
	}

	indexFilenames := make([]string, len(fileHandles))
	for idx, handle := range fileHandles {
		indexFilenames[idx] = handle.Name()
	}

	for i := len(indexFilenames) - 1; i >= 0; i-- {
		if strings.HasPrefix(indexFilenames[i], mapping) {
			return indexFilenames[i], nil
		}
	}

	return "", fmt.Errorf("No mapping files found for %s", mapping)
}

func extractBaseName(filename string) (string, error) {
	if !strings.HasSuffix(filename, ".json") {
		return "", fmt.Errorf("Invalid mapping file %s -- must be JSON file", filename)
	}

	sansExtension := filename[:len(filename)-5]
	return strings.Split(sansExtension, "__")[0], nil
}
