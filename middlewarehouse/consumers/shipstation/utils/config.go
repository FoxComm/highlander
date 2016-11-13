package utils

import (
	"errors"
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	ZookeeperURL       string
	SchemaRegistryURL  string
	Topic              string
	MiddlewarehouseURL string
	PollingInterval    time.Duration
	ApiKey             string
	ApiSecret          string
	Partition          int
}

func MakeConfig() (Config, error) {

	var err error
	config := Config{}

	config.PollingInterval, err = time.ParseDuration(os.Getenv("POLLING_INTERVAL"))
	fmt.Printf("INTERVAL: %v\n", config.PollingInterval)
	if err != nil {
		return config, fmt.Errorf("Unable to parse POLLING_INTERVAL with error %s", err.Error())
	}

	config.SchemaRegistryURL = os.Getenv("SCHEMA_REGISTRY_URL")
	if config.SchemaRegistryURL == "" {
		return config, errors.New("Unable to find SCHEMA_REGISTRY_URL in env")
	}

	config.ZookeeperURL = os.Getenv("ZOOKEEPER_URL")
	if config.ZookeeperURL == "" {
		return config, errors.New("Unable to find ZOOKEEPER_URL in env")
	}

	config.Topic = os.Getenv("TOPIC")
	if config.Topic == "" {
		return config, errors.New("Unable to find TOPIC in env")
	}

	config.MiddlewarehouseURL = os.Getenv("MIDDLEWAREHOUSE_URL")
	if config.MiddlewarehouseURL == "" {
		return config, errors.New("MIDDLEWAREHOUSE_URL is not set")
	}

	config.ApiKey = os.Getenv("API_KEY")
	if config.ApiKey == "" {
		return config, errors.New("Unable to find API_KEY in env")
	}

	config.ApiSecret = os.Getenv("API_SECRET")
	if config.ApiSecret == "" {
		return config, errors.New("Unable to find API_SECRET in env")
	}

	config.Partition, err = strconv.Atoi(os.Getenv("PARTITION"))
	if err != nil {
		return config, errors.New("Unable to parse valid PARTITION in env")
	}

	return config, nil
}
