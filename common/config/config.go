package config

import (
	"errors"
	"os"
)

// SiteConfig is a structure representing all of the site level configurations
// that middlewarehouse can have. All values are expected to be set at initialization.
type SiteConfig struct {
	KafkaBroker       string
	ZookeeperURL      string
	SchemaRegistryURL string
}

// Config stores all site level configurations for the application.
var Config *SiteConfig

func InitializeSiteConfig() error {
	kafkaBroker := os.Getenv("KAFKA_BROKER")
	if kafkaBroker == "" {
		return errors.New("KAFKA_BROKER not found in the environment")
	}

	zookeeperURL := os.Getenv("ZOOKEEPER_URL")
	if zookeeperURL == "" {
		return errors.New("ZOOKEEPER_URL not found in the environment")
	}

	schemaRegistryURL := os.Getenv("SCHEMA_REGISTRY_URL")
	if schemaRegistryURL == "" {
		return errors.New("SCHEMA_REGISTRY_URL not found in the environment")
	}

	Config = &SiteConfig{kafkaBroker, zookeeperURL, schemaRegistryURL}
	return nil
}
