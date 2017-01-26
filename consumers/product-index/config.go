package main

import (
	"os"
	"encoding/json"
	"errors"
)

type IndexerConfig struct {
	visualVariants []string `json:"visualVariants"`
}


func MakeIndexerConfig() (IndexerConfig, error) {
	config := IndexerConfig{}
	var err error

	visualVariantsStr := os.Getenv("VISUAL_VARIANTS")
	if visualVariantsStr == "" {
		return config, errors.New("Unable to find VISUAL_VARIANTS in env")
	}
	err = json.Unmarshal([]byte(visualVariantsStr), &config.visualVariants)
	if err != nil {
		return config, err
	}

	return config, nil
}
