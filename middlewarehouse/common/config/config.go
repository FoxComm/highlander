package config

import (
	"errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"os"
)

// SiteConfig is a structure representing all of the site level configurations
// that middlewarehouse can have. All values are expected to be set at initialization.
type SiteConfig struct {
	KafkaBroker       string
	SchemaRegistryURL string
}

// Config stores all site level configurations for the application.
var Config *SiteConfig

func InitializeSiteConfig() exceptions.IException {
	kafkaBroker := os.Getenv("KAFKA_BROKER")
	if kafkaBroker == "" {
		return exceptions.NewBadConfigurationException(
			errors.New("KAFKA_BROKER not found in the environment"),
		)
	}

	schemaRegistryURL := os.Getenv("SCHEMA_REGISTRY_URL")
	if schemaRegistryURL == "" {
		return exceptions.NewBadConfigurationException(
			errors.New("SCHEMA_REGISTRY_URL not found in the environment"),
		)
	}

	Config = &SiteConfig{kafkaBroker, schemaRegistryURL}
	return nil
}
