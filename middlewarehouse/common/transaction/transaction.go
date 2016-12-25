package transaction

import (
	"database/sql"
	"fmt"
	"reflect"

	"github.com/jinzhu/gorm"
)

type Transaction struct {
	db      *gorm.DB
	wrapped bool
	Error   error
}

func NewTransaction(db *gorm.DB) *Transaction {
	_, inTxn := db.CommonDB().(*sql.Tx)

	return &Transaction{db, inTxn, nil}
}

func (txn *Transaction) DB() *gorm.DB {
	return txn.db
}

// Begin transaction
func (txn *Transaction) Begin() *Transaction {
	return txn.runTransaction("Begin", false)
}

// Commit transaction
func (txn *Transaction) Commit() *Transaction {
	return txn.runTransaction("Commit", true)
}

// Rollback transaction
func (txn *Transaction) Rollback() *Transaction {
	return txn.runTransaction("Rollback", true)
}

// Rollback transaction
func (txn *Transaction) Create(value interface{}) *gorm.DB {
	return txn.db.Create(value)
}

// Rollback transaction
func (txn *Transaction) Set(name string, value interface{}) *gorm.DB {
	return txn.db.Set(name, value)
}

//func (txn *Transaction) runQuery(methodName string, value interface{}) *gorm.DB {
//	methodValue := reflect.ValueOf(txn.db).MethodByName(methodName)
//	if !methodValue.IsValid() {
//		panic(fmt.Sprintf("Can't call method %s of Transaction. Not implemented", methodName))
//	}
//
//	result := methodValue.Call([]reflect.Value{reflect.ValueOf(value)})
//
//	return result[0].Interface().(*gorm.DB)
//}

func (txn *Transaction) runTransaction(methodName string, whenTxn bool) *Transaction {
	if txn.wrapped {
		return txn
	}

	if _, isTxn := txn.db.CommonDB().(*sql.Tx); isTxn == whenTxn {
		methodValue := reflect.ValueOf(txn.db).MethodByName(methodName)
		if !methodValue.IsValid() {
			panic(fmt.Sprintf("Can't call method %s of Transaction", methodName))
		}

		result := methodValue.Call([]reflect.Value{})

		txn.db = result[0].Interface().(*gorm.DB)
		txn.Error = txn.db.Error
	}

	return txn
}
