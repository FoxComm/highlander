package transaction

import (
	"database/sql"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type TransactionTestSuite struct {
	suite.Suite
	db *gorm.DB
}

func TestTransactionTestSuiteSuite(t *testing.T) {
	suite.Run(t, new(TransactionTestSuite))
}

func (suite *TransactionTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
}

func (suite *TransactionTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *TransactionTestSuite) Test_Begin() {
	txn := NewTransaction(suite.db)

	suite.Nil(txn.Begin().Error)

	_, isTxn := txn.DB().CommonDB().(*sql.Tx)
	suite.True(isTxn)
}

func (suite *TransactionTestSuite) Test_Commit() {
	txn := NewTransaction(suite.db)
	txn.Begin()

	suite.Nil(txn.Commit().Error)
	suite.NotNil(txn.Rollback().Error)
}

func (suite *TransactionTestSuite) Test_Rollback() {
	txn := NewTransaction(suite.db)
	txn.Begin()

	suite.Nil(txn.Rollback().Error)
}

func (suite *TransactionTestSuite) Test_InTransaction() {
	realTxn := suite.db.Begin()
	txn := NewTransaction(realTxn)

	suite.Nil(txn.Begin().Error)
	suite.Nil(txn.Commit().Error)
	suite.Nil(txn.Rollback().Error)

	realTxn.Rollback()
}
