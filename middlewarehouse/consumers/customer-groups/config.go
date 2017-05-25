package main

import (
	"fmt"
	"os"
	"time"
)

const (
	ElasticURLKey   = "ELASTIC_URL"
	PollingInterval = "POLLING_INTERVAL"
	SleepInterval   = "SLEEP_INTERVAL"
)

const (
	DefaultSleepInterval = "5s"
)

type agentConfig struct {
	ElasticURL      string
	ElasticIndex    string
	PollingInterval time.Duration
	SleepInterval   time.Duration
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

	config.PollingInterval, err = time.ParseDuration(os.Getenv(PollingInterval))
	if err != nil {
		return config, fmt.Errorf(parseErrorMsg, PollingInterval, err.Error())
	}

	config.SleepInterval, err = time.ParseDuration(os.Getenv(SleepInterval))
	if err != nil {
		config.SleepInterval, err = time.ParseDuration(DefaultSleepInterval)
		if err != nil {
			return config, fmt.Errorf(parseErrorMsg, SleepInterval, err.Error())
		}
	}

	return config, nil
}
