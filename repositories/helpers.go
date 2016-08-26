package repositories

import (
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
)

func CreateDbMock() (*gorm.DB, sqlmock.Sqlmock) {
	sqldb, mock, err := sqlmock.New()

	if err != nil {
		panic(err)
	}

	db, err := gorm.Open("sqlmock", sqldb)

	if err != nil {
		panic(err)
	}

	return db, mock
}
