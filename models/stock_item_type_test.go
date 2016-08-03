package models

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type stockItemTypeTestSuite struct {
	suite.Suite
	assert *assert.Assertions
	types  itemType
}

func TestStockItemTypeSuite(t *testing.T) {
	suite.Run(t, new(stockItemTypeTestSuite))
}

func (suite *stockItemTypeTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
	suite.types = StockItemTypes()
}

func (suite *stockItemTypeTestSuite) Test_StockItemTypes() {
	types := StockItemTypes()

	suite.assert.Equal(uint(1), types.Sellable)
	suite.assert.Equal(uint(2), types.NonSellable)
	suite.assert.Equal(uint(3), types.Backorder)
	suite.assert.Equal(uint(4), types.Preorder)
}

func (suite *stockItemTypeTestSuite) Test_StockItemTypes_Immutable() {
	types := StockItemTypes()

	types.Sellable = 2

	suite.assert.Equal(uint(1), StockItemTypes().Sellable)
}

var fromStringTest = []struct {
	in  string
	out uint
}{
	{"Sellable", uint(1)},
	{"Non-sellable", uint(2)},
	{"Backorder", uint(3)},
	{"Preorder", uint(4)},
}

func (suite *stockItemTypeTestSuite) Test_StockItemTypeFromString() {
	for _, td := range fromStringTest {
		typeId := StockItemTypeFromString(td.in)
		suite.assert.Equal(td.out, typeId)
	}
}
