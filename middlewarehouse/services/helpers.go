package services

import (
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
)

func CreateDbMock() (*gorm.DB, sqlmock.Sqlmock) {
	sqldb, mock, _ := sqlmock.New()

	db, _ := gorm.Open("sqlmock", sqldb)

	return db, mock
}
