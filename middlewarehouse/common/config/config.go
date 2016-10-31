package config

import (
	"fmt"
	"os"
)

// SiteConfig is a structure representing all of the site level configurations
// that middlewarehouse can have. All values are expected to be set at initialization.
type SiteConfig struct {
	KafkaBroker       string
	SchemaRegistryURL string
}

const (
	KAFKA_BROKER        = "KAFKA_BROKER"
	SCHEMA_REGISTRY_URL = "SCHEMA_REGISTRY_URL"

	ErrorEnvironmentVariableNotFound = "%s not found in the environment"
)

// Config stores all site level configurations for the application.
var Config *SiteConfig

func InitializeSiteConfig() error {
	if Config != nil {
		return nil
	}

	kafkaBroker := os.Getenv(KAFKA_BROKER)
	if kafkaBroker == "" {
		return fmt.Errorf(ErrorEnvironmentVariableNotFound, KAFKA_BROKER)
	}

	schemaRegistryURL := os.Getenv(SCHEMA_REGISTRY_URL)
	if schemaRegistryURL == "" {
		return fmt.Errorf(ErrorEnvironmentVariableNotFound, SCHEMA_REGISTRY_URL)
	}

	Config = &SiteConfig{kafkaBroker, schemaRegistryURL}
	return nil
}
