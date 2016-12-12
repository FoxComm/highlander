package consumers

import (
	"errors"
	"github.com/FoxComm/metamorphosis"
	"os"
	"strconv"
)

type ConsumerConfig struct {
	SchemaRepositoryURL string
	ZookeeperURL        string
	MiddlewarehouseURL  string
	Topic               string
	Partition           int
	OffsetResetStrategy string
}

func MakeConsumerConfig() (ConsumerConfig, error) {
	config := ConsumerConfig{}

	config.SchemaRepositoryURL = os.Getenv("SCHEMA_REPOSITORY_URL")
	if config.SchemaRepositoryURL == "" {
		return config, errors.New("Unable to find SCHEMA_REPOSITORY_URL in env")
	}

	config.ZookeeperURL = os.Getenv("ZOOKEEPER_URL")
	if config.ZookeeperURL == "" {
		return config, errors.New("Unable to find ZOOKEEPER_URL in env")
	}

	config.MiddlewarehouseURL = os.Getenv("MWH_URL")
	if config.MiddlewarehouseURL == "" {
		return config, errors.New("Unable to find MWH_URL in env")
	}

	config.Topic = os.Getenv("TOPIC")
	if config.Topic == "" {
		return config, errors.New("Unable to find TOPIC in env")
	}

	config.OffsetResetStrategy = os.Getenv("OFFSET_RESET_STRATEGY")
	if invalidOffsetResetStrategy(config.OffsetResetStrategy) {
		return config, errors.New("Unable to find TOPIC in env")
	} else if config.OffsetResetStrategy == "" {
		config.OffsetResetStrategy = metamorphosis.OffsetResetLargest
	}

	var err error
	config.Partition, err = strconv.Atoi(os.Getenv("PARTITION"))
	if err != nil {
		return config, errors.New("Unable to parse valid PARTITION in env")
	}

	return config, nil
}

func invalidOffsetResetStrategy(value string) bool {
	return value != "" && value != metamorphosis.OffsetResetLargest && value != metamorphosis.OffsetResetSmallest
}
