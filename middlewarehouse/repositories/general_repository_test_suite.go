package repositories

import (
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type GeneralRepositoryTestSuite struct {
	suite.Suite
	db *gorm.DB
}
