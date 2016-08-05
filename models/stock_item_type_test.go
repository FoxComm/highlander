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
