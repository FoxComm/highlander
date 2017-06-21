package utils

import (
	"fmt"
	"os"
	"strconv"
)

const (
	errEnvVarNotFound = "%s not found in the environment"

	phoenixDatabaseName     = "PHX_DATABASE_NAME"
	phoenixDatabaseHost     = "PHX_DATABASE_HOST"
	phoenixDatabaseUser     = "PHX_DATABASE_USER"
	phoenixDatabasePassword = "PHX_DATABASE_PASSWORD"
	phoenixDatabaseSSL      = "PHX_DATABASE_SSL"

	port = "PORT"
)

type Config struct {
	PhxDatabaseName     string
	PhxDatabaseHost     string
	PhxDatabaseUser     string
	PhxDatabasePassword string
	PhxDatabaseSSL      string

	Port int
}

func NewConfig() (*Config, error) {
	config := &Config{}
	var err error

	config.PhxDatabaseName, err = parseEnvVar(phoenixDatabaseName, nil)
	config.PhxDatabaseHost, err = parseEnvVar(phoenixDatabaseHost, err)
	config.PhxDatabaseUser, err = parseEnvVar(phoenixDatabaseUser, err)
	config.PhxDatabaseSSL, err = parseEnvVar(phoenixDatabaseSSL, err)
	config.PhxDatabasePassword = os.Getenv(phoenixDatabasePassword)

	port, err := parseEnvVar(port, err)
	config.Port, err = strconv.Atoi(port)

	return config, err
}

func parseEnvVar(varName string, err error) (string, error) {
	if err != nil {
		return "", err
	}

	str := os.Getenv(varName)
	if str == "" {
		return "", fmt.Errorf(errEnvVarNotFound, varName)
	}

	return str, nil
}
