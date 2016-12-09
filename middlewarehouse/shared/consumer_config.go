package shared

import (
	"fmt"
	"os"
)

// ConsumerConfig represents configuration for consumer that must
// be present at execution time, but are specific to this consumer.
type ConsumerConfig struct {
	PhoenixURL      string
	PhoenixUser     string
	PhoenixPassword string
}

const consumerErrorMsg = "%s not found in env"

func MakeConsumerConfig() (ConsumerConfig, error) {
	config := ConsumerConfig{}

	config.PhoenixURL = os.Getenv("PHOENIX_URL")
	if config.PhoenixURL == "" {
		return config, fmt.Errorf(consumerErrorMsg, "PHOENIX_URL")
	}

	config.PhoenixUser = os.Getenv("PHOENIX_USER")
	if config.PhoenixUser == "" {
		return config, fmt.Errorf(consumerErrorMsg, "PHOENIX_USER")
	}

	config.PhoenixPassword = os.Getenv("PHOENIX_PASSWORD")
	if config.PhoenixPassword == "" {
		return config, fmt.Errorf(consumerErrorMsg, "PHOENIX_PASSWORD")
	}

	return config, nil
}
