package cmd

import (
	"fmt"
	"io/ioutil"

	yaml "gopkg.in/yaml.v2"
)

// Config is a representation of the main configuration file.
type Config struct {
	ElasticConnection struct {
		Host string `yaml:"host"`
		Port string `yaml:"port"`
	} `yaml:"es_connection"`
	ElasticIndices []struct {
		Name     string             `yaml:"name"`
		Searches []SearchDefinition `yaml:"searches"`
	} `yaml:"es_indices"`
}

// NewConfig loads the configuration structure from a file.
func NewConfig(filename string) (*Config, error) {
	cfgContents, err := ioutil.ReadFile(filename)
	if err != nil {
		return nil, err
	}

	config := &Config{}
	if err := yaml.Unmarshal(cfgContents, config); err != nil {
		return nil, err
	}

	return config, nil
}

func (c *Config) SearchDefinitionByMapping(mappingName string) (*SearchDefinition, error) {
	for _, index := range c.ElasticIndices {
		for _, search := range index.Searches {
			if search.Mapping == mappingName {
				return &search, nil
			}
		}
	}

	return nil, fmt.Errorf("No search definition for mapping %s", mappingName)
}

// ElasticURL returns the HTTP URL used to connect to the ES cluster.
func (c *Config) ElasticURL() string {
	return fmt.Sprintf("http://%s:%s", c.ElasticConnection.Host, c.ElasticConnection.Port)
}

type SearchDefinition struct {
	Name    string `yaml:"name"`
	Index   string `yaml:"-"`
	Scoped  bool   `yaml:"scoped"`
	Mapping string `yaml:"mapping"`
}
