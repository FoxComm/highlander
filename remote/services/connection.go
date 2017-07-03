package services

import (
	"fmt"

	"github.com/FoxComm/highlander/remote/utils"
	"github.com/jinzhu/gorm"
	_ "github.com/lib/pq"
)

func NewPhoenixConnection(config *utils.Config) (*gorm.DB, error) {
	connStr := fmt.Sprintf(
		"host=%s user=%s password=%s dbname=%s sslmode=%s",
		config.PhxDatabaseHost,
		config.PhxDatabaseUser,
		config.PhxDatabasePassword,
		config.PhxDatabaseName,
		config.PhxDatabaseSSL)

	phxDB, err := gorm.Open("postgres", connStr)
	if err != nil {
		return nil, fmt.Errorf("Unable to connect to Phoenix DB with error %s", err.Error())
	}

	return phxDB, nil
}
