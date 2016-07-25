package services

import (
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type GeneralServiceTestSuite struct {
	suite.Suite
	assert *assert.Assertions
	db     *gorm.DB
	mock   sqlmock.Sqlmock
}

func CreateDbMock() (*gorm.DB, sqlmock.Sqlmock) {
	sqldb, mock, _ := sqlmock.New()

	db, _ := gorm.Open("sqlmock", sqldb)

	return db, mock
}
