package consumers

import (
	"errors"
	"os"
	"strconv"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type ConsumerConfig struct {
	SchemaRepositoryURL string
	ZookeeperURL        string
	MiddlewarehouseURL  string
	Topic               string
	Partition           int
}

func MakeConsumerConfig() (ConsumerConfig, exceptions.IException) {
	config := ConsumerConfig{}

	config.SchemaRepositoryURL = os.Getenv("SCHEMA_REPOSITORY_URL")
	if config.SchemaRepositoryURL == "" {
		return config, exceptions.NewBadConfigurationException(errors.New("Unable to find SCHEMA_REPOSITORY_URL in env"))
	}

	config.ZookeeperURL = os.Getenv("ZOOKEEPER_URL")
	if config.ZookeeperURL == "" {
		return config, exceptions.NewBadConfigurationException(errors.New("Unable to find ZOOKEEPER_URL in env"))
	}

	config.MiddlewarehouseURL = os.Getenv("MWH_URL")
	if config.MiddlewarehouseURL == "" {
		return config, exceptions.NewBadConfigurationException(errors.New("Unable to find MWH_URL in env"))
	}

	config.Topic = os.Getenv("TOPIC")
	if config.Topic == "" {
		return config, exceptions.NewBadConfigurationException(errors.New("Unable to find TOPIC in env"))
	}

	var err error
	config.Partition, err = strconv.Atoi(os.Getenv("PARTITION"))
	if err != nil {
		return config, exceptions.NewBadConfigurationException(errors.New("Unable to parse valid PARTITION in env"))
	}

	return config, nil
}
