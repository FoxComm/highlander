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
	DatabaseConnections []map[string]struct {
		Database string `yaml:"database"`
		Host     string `yaml:"host"`
		User     string `yaml:"user"`
		Password string `yaml:"password"`
		SSLMode  string `yaml:"sslmode"`
	} `yaml:"db_connections"`
	ElasticIndices []struct {
		Name     string            `yaml:"name"`
		Searches []AliasDefinition `yaml:"searches"`
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

func (c *Config) ElasticAliases() []AliasDefinition {
	aliases := []AliasDefinition{}
	for _, index := range c.ElasticIndices {
		for _, alias := range index.Searches {
			alias.Index = index.Name
			aliases = append(aliases, alias)
		}
	}

	return aliases
}

// ElasticURL returns the HTTP URL used to connect to the ES cluster.
func (c *Config) ElasticURL() string {
	return fmt.Sprintf("http://%s:%s", c.ElasticConnection.Host, c.ElasticConnection.Port)
}

// DatabaseConnectionStrings returns the database connections in the form needed
// to make a connection to PostgreSQL.
func (c *Config) DatabaseConnectionStrings() map[string]string {
	conns := map[string]string{}

	for _, connMap := range c.DatabaseConnections {
		for connName, connDefn := range connMap {
			conn := fmt.Sprintf("dbname=%s", connDefn.Database)

			if connDefn.Host != "" {
				conn = fmt.Sprintf("%s host=%s", conn, connDefn.Host)
			}
			if connDefn.User != "" {
				conn = fmt.Sprintf("%s user=%s", conn, connDefn.User)
			}
			if connDefn.Password != "" {
				conn = fmt.Sprintf("%s password=%s", conn, connDefn.Password)
			}
			if connDefn.SSLMode != "" {
				conn = fmt.Sprintf("%s sslmode=%s", conn, connDefn.SSLMode)
			} else {
				conn = fmt.Sprintf("%s sslmode=disable", conn)
			}

			conns[connName] = conn
		}
	}

	return conns
}

type AliasDefinition struct {
	Name   string `yaml:"name"`
	Index  string `yaml:"-"`
	Scoped bool   `yaml:"scoped"`
	View   string `yaml:"view"`
}
