package shared

import (
	"fmt"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"os"
)

// CaptureConsumerConfig represents configuration for this consumer that must
// be present at execution time, but are specific to this consumer.
type CaptureConsumerConfig struct {
	PhoenixURL      string
	PhoenixUser     string
	PhoenixPassword string
}

const consumerErrorMsg = "%s not found in env"

func MakeCaptureConsumerConfig() (CaptureConsumerConfig, exceptions.IException) {
	config := CaptureConsumerConfig{}

	config.PhoenixURL = os.Getenv("PHOENIX_URL")
	if config.PhoenixURL == "" {
		return config, exceptions.NewBadConfigurationException(fmt.Errorf(consumerErrorMsg, "PHOENIX_URL"))
	}

	config.PhoenixUser = os.Getenv("PHOENIX_USER")
	if config.PhoenixUser == "" {
		return config, exceptions.NewBadConfigurationException(fmt.Errorf(consumerErrorMsg, "PHOENIX_USER"))
	}

	config.PhoenixPassword = os.Getenv("PHOENIX_PASSWORD")
	if config.PhoenixPassword == "" {
		return config, exceptions.NewBadConfigurationException(fmt.Errorf(consumerErrorMsg, "PHOENIX_PASSWORD"))
	}

	return config, nil
}
