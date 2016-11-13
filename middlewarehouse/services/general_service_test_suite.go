package services

import (
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type GeneralServiceTestSuite struct {
	suite.Suite
	db *gorm.DB
}
