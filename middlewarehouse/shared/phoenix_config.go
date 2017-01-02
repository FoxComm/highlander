package shared

import (
	"fmt"
	"os"
)

// PhoenixConfig represents configuration for consumer that must
// be present at execution time, but are specific to this consumer.
type PhoenixConfig struct {
	URL      string
	User     string
	Password string
}

const consumerErrorMsg = "%s not found in env"

func MakePhoenixConfig() (PhoenixConfig, error) {
	config := PhoenixConfig{}

	config.URL = os.Getenv("PHOENIX_URL")
	if config.URL == "" {
		return config, fmt.Errorf(consumerErrorMsg, "PHOENIX_URL")
	}

	config.User = os.Getenv("PHOENIX_USER")
	if config.User == "" {
		return config, fmt.Errorf(consumerErrorMsg, "PHOENIX_USER")
	}

	config.Password = os.Getenv("PHOENIX_PASSWORD")
	if config.Password == "" {
		return config, fmt.Errorf(consumerErrorMsg, "PHOENIX_PASSWORD")
	}

	return config, nil
}
