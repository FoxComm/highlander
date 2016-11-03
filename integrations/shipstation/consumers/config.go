package consumers

import (
	"errors"
	"os"
	"strconv"
)

type ConsumerConfig struct {
	SchemaRegistryURL string
	ZookeeperURL        string
	Topic               string
	ApiKey              string
	ApiSecret           string
	Partition           int
}

func MakeConsumerConfig() (ConsumerConfig, error) {
	config := ConsumerConfig{}

	config.SchemaRegistryURL = os.Getenv("SCHEMA_REGISTRY_URL")
	if config.SchemaRegistryURL == "" {
		return config, errors.New("Unable to find SCHEMA_REPOSITORY_URL in env")
	}

	config.ZookeeperURL = os.Getenv("ZOOKEEPER_URL")
	if config.ZookeeperURL == "" {
		return config, errors.New("Unable to find ZOOKEEPER_URL in env")
	}

	config.Topic = os.Getenv("TOPIC")
	if config.Topic == "" {
		return config, errors.New("Unable to find TOPIC in env")
	}

	config.ApiKey = os.Getenv("API_KEY")
	if config.Topic == "" {
		return config, errors.New("Unable to find API_KEY in env")
	}

	config.ApiKey = os.Getenv("API_SECRET")
	if config.Topic == "" {
		return config, errors.New("Unable to find API_SECRET in env")
	}

	var err error
	config.Partition, err = strconv.Atoi(os.Getenv("PARTITION"))
	if err != nil {
		return config, errors.New("Unable to parse valid PARTITION in env")
	}

	return config, nil
}
