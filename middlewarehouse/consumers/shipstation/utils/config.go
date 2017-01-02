package utils

import (
	"errors"
	"fmt"
	"os"
	"time"
)

type Config struct {
	PollingInterval time.Duration
	ApiKey          string
	ApiSecret       string
}

func MakeConfig() (Config, error) {

	var err error
	config := Config{}

	config.PollingInterval, err = time.ParseDuration(os.Getenv("POLLING_INTERVAL"))
	fmt.Printf("INTERVAL: %v\n", config.PollingInterval)
	if err != nil {
		return config, fmt.Errorf("Unable to parse POLLING_INTERVAL with error %s", err.Error())
	}

	config.ApiKey = os.Getenv("API_KEY")
	if config.ApiKey == "" {
		return config, errors.New("Unable to find API_KEY in env")
	}

	config.ApiSecret = os.Getenv("API_SECRET")
	if config.ApiSecret == "" {
		return config, errors.New("Unable to find API_SECRET in env")
	}

	return config, nil
}
