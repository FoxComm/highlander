package repositories

import (
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type GeneralRepositoryTestSuite struct {
	suite.Suite
	assert *assert.Assertions
	db     *gorm.DB
	mock   sqlmock.Sqlmock
}
