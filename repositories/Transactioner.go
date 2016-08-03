package repositories

import "github.com/jinzhu/gorm"

type ITransactioner interface {
	Begin() *gorm.DB
}

type DBTransactioner struct {
	db *gorm.DB
}

func (txnr *DBTransactioner) Begin() *gorm.DB {
	return txnr.db.Begin()
}

var dbTransactioner *DBTransactioner

func NewDBTransactioner(db *gorm.DB) *DBTransactioner {
	if dbTransactioner != nil {
		return dbTransactioner
	}

	return &DBTransactioner{db}
}
