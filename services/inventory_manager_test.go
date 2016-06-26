package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/common/store"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type InventoryManagerTestSuite struct {
	suite.Suite
	mgr *InventoryMgr
}

func TestInventoryManagerSuite(t *testing.T) {
	suite.Run(t, new(InventoryManagerTestSuite))
}

func (suite *InventoryManagerTestSuite) SetupTest() {
	tasks.TruncateTables([]string{"stock_items"})

	ctx := store.StoreContext{StoreID: 1}

	var err error
	suite.mgr, err = NewInventoryMgr(&ctx)
	assert.Nil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreation() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-SKU"}
	resp, err := suite.mgr.CreateStockItem(payload)
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), uint(1), resp.ID)
	}
}

func (suite *InventoryManagerTestSuite) TestFindByID() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-SKU"}
	resp, err := suite.mgr.CreateStockItem(payload)
	if assert.Nil(suite.T(), err) {
		item, err := suite.mgr.FindStockItemByID(resp.ID)
		if assert.Nil(suite.T(), err) {
			assert.Equal(suite.T(), "TEST-SKU", item.SKU)
		}
	}
}

func (suite *InventoryManagerTestSuite) TestEmptySKU() {
	payload := &payloads.StockItem{StockLocationID: 1}
	_, err := suite.mgr.CreateStockItem(payload)
	assert.NotNil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreateDuplicateSKU() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-SKU"}
	_, err := suite.mgr.CreateStockItem(payload)
	assert.Nil(suite.T(), err)

	_, err = suite.mgr.CreateStockItem(payload)
	assert.NotNil(suite.T(), err)
}
