package config

import (
	"errors"
	"os"
)

// SiteConfig is a structure representing all of the site level configurations
// that middlewarehouse can have. All values are expected to be set at initialization.
type SiteConfig struct {
	PhoenixURL string
	PhoenixJWT string
}

// Config stores all site level configurations for the application.
var Config *SiteConfig

func InitializeSiteConfig() error {
	phoenixURL := os.Getenv("PHOENIX_URL")
	if phoenixURL == "" {
		return errors.New("PHOENX_URL not found in the environment.")
	}

	phoenixJWT := os.Getenv("JWT")
	if phoenixJWT == "" {
		return errors.New("JWT not found in the environment.")
	}

	Config = &SiteConfig{phoenixURL, phoenixJWT}
	return nil
}
