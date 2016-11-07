package utils

import (
	"errors"
	"fmt"
	"os"
	"time"
)

var Config SiteConfig

type SiteConfig struct {
	ZookeeperURL       string
	SchemaRegistryURL  string
	MiddlewarehouseURL string
	PollingInterval    time.Duration
}

func InitializeConfig() error {
	zookeeperURL := os.Getenv("ZOOKEEPER_URL")
	if zookeeperURL == "" {
		return errors.New("ZOOKEEPER_URL is not set")
	}

	schemaRegistryURL := os.Getenv("SCHEMA_REGISTRY_URL")
	if schemaRegistryURL == "" {
		return errors.New("SCHEMA_REGISTRY_URL is not set")
	}

	middlewarehouseURL := os.Getenv("MIDDLEWAREHOUSE_URL")
	if middlewarehouseURL == "" {
		return errors.New("MIDDLEWAREHOUSE_URL is not set")
	}

	pollingInterval, err := time.ParseDuration(os.Getenv("POLLING_INTERVAL"))
	if err != nil {
		return fmt.Errorf("Unable to parse POLLING_INTERVAL with error %s", err.Error())
	}

	Config = SiteConfig{zookeeperURL, schemaRegistryURL, middlewarehouseURL, pollingInterval}
	return nil
}
