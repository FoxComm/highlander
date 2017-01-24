package main

import (
	"fmt"
	"os"
	"time"
)

const (
	ElasticURLKey   = "ES_URL"
	ElasticIndexKey = "ES_INDEX"
	PollingInterval = "POLLING_INTERVAL"
)

type agentConfig struct {
	ElasticURL      string
	ElasticIndex    string
	PollingInterval time.Duration
}

const configErrorMsg = "%s not found in env"
const parseErrorMsg = "Unable to parse %s with error %s"

func makeAgentConfig() (agentConfig, error) {
	var err error
	config := agentConfig{}

	config.ElasticURL = os.Getenv(ElasticURLKey)
	if config.ElasticURL == "" {
		return config, fmt.Errorf(configErrorMsg, ElasticURLKey)
	}

	config.ElasticIndex = os.Getenv(ElasticIndexKey)
	if config.ElasticIndex == "" {
		return config, fmt.Errorf(configErrorMsg, ElasticIndexKey)
	}

	config.PollingInterval, err = time.ParseDuration(os.Getenv(PollingInterval))
	if err != nil {
		return config, fmt.Errorf(parseErrorMsg, PollingInterval, err.Error())
	}

	return config, nil
}
