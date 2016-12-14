package config

import (
	"fmt"
	"os"
)

// AppConfig is a structure representing all of the site level configurations
// that middlewarehouse can have. All values are expected to be set at initialization.
type AppConfig struct {
	Port              string
	KafkaBroker       string
	SchemaRegistryURL string
}

const (
	PORT                = "PORT"
	KAFKA_BROKER        = "KAFKA_BROKER"
	SCHEMA_REGISTRY_URL = "SCHEMA_REGISTRY_URL"

	ZIPKIN_SERVER_URL = "ZIPKIN_SERVER_URL"

	ErrorEnvironmentVariableNotFound = "%s not found in the environment"
)

func NewAppConfig() (*AppConfig, error) {

	port := os.Getenv(PORT)
	if port == "" {
		return nil, fmt.Errorf(ErrorEnvironmentVariableNotFound, PORT)
	}

	kafkaBroker := os.Getenv(KAFKA_BROKER)
	if kafkaBroker == "" {
		return nil, fmt.Errorf(ErrorEnvironmentVariableNotFound, KAFKA_BROKER)
	}

	schemaRegistryURL := os.Getenv(SCHEMA_REGISTRY_URL)
	if schemaRegistryURL == "" {
		return nil, fmt.Errorf(ErrorEnvironmentVariableNotFound, SCHEMA_REGISTRY_URL)
	}

	zipkinHttpEndpoint := os.Getenv(ZIPKIN_SERVER_URL)
	if zipkinHttpEndpoint == "" {
		return nil, fmt.Errorf(ErrorEnvironmentVariableNotFound, ZIPKIN_SERVER_URL)
	}

	return &AppConfig{port, kafkaBroker, schemaRegistryURL}, nil
}

type TracerConfig struct {
	ZipkinHttpEndpoint string
}

func NewTracerConfig() (*TracerConfig, error) {
	zipkinHttpEndpoint := os.Getenv(ZIPKIN_SERVER_URL)
	if zipkinHttpEndpoint == "" {
		return nil, fmt.Errorf(ErrorEnvironmentVariableNotFound, ZIPKIN_SERVER_URL)
	}

	return &TracerConfig{zipkinHttpEndpoint}, nil
}
