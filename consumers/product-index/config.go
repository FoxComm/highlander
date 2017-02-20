package main

import (
	"encoding/json"
	"errors"
	"os"
)

type IndexerConfig struct {
	VisualVariants []string `json:"visualVariants"`
	consulAddress  string
	consulScheme   string
}

func MakeIndexerConfig() (IndexerConfig, error) {
	config := IndexerConfig{}

	config.consulAddress = os.Getenv("CONSUL_ADDRESS")
	config.consulScheme = os.Getenv("CONSUL_SCHEME")
	var err error

	visualVariantsStr := os.Getenv("VISUAL_VARIANTS")
	if visualVariantsStr == "" {
		return config, errors.New("Unable to find VISUAL_VARIANTS in env")
	}
	err = json.Unmarshal([]byte(visualVariantsStr), &config.VisualVariants)
	if err != nil {
		return config, err
	}

	return config, nil
}
