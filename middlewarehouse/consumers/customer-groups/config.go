package main

import (
	"fmt"
	"os"
	"time"
)

const (
	ElasticURLKey   = "ELASTIC_URL"
	PollingInterval = "POLLING_INTERVAL"
	MailchimpAPIKey = "MAILCHIMP_API_KEY"
	MailchimpListId = "MAILCHIMP_LIST_ID"
)

type agentConfig struct {
	ElasticURL      string
	ElasticIndex    string
	PollingInterval time.Duration
	MailchimpAPIKey string
	MailchimpListId string
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

	config.MailchimpAPIKey = os.Getenv(MailchimpAPIKey)
	if config.MailchimpAPIKey == "" {
		return config, fmt.Errorf(configErrorMsg, MailchimpAPIKey)
	}

	config.MailchimpListId = os.Getenv(MailchimpListId)
	if config.MailchimpListId == "" {
		return config, fmt.Errorf(configErrorMsg, MailchimpListId)
	}

	return config, nil
}
