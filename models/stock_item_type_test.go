package models

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type stockItemTypeTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestStockItemTypeSuite(t *testing.T) {
	suite.Run(t, new(stockItemTypeTestSuite))
}

func (suite *stockItemTypeTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *stockItemTypeTestSuite) Test_StockItemTypes() {
	types := StockItemTypes()

	println("HEU")
	suite.assert.Equal(1, types.Sellable)
	suite.assert.Equal(2, types.NonSellable)
	suite.assert.Equal(3, types.Backorder)
	suite.assert.Equal(4, types.Preorder)
}

func (suite *stockItemTypeTestSuite) Test_StockItemTypes_Immutable() {
	types := StockItemTypes()

	types.Sellable = 2

	suite.assert.Equal(1, StockItemTypes().Sellable)
}
