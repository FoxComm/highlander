package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/common/store"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type InventoryManagerTestSuite struct {
	suite.Suite
	mgr      *InventoryMgr
	itemResp *responses.StockItem
	db       *gorm.DB
}

func TestInventoryManagerSuite(t *testing.T) {
	suite.Run(t, new(InventoryManagerTestSuite))
}

func (suite *InventoryManagerTestSuite) SetupTest() {
	var err error
	suite.db, err = config.DefaultConnection()
	assert.Nil(suite.T(), err)

	assert.Nil(suite.T(), err)
	tasks.TruncateTables([]string{"stock_items", "stock_item_units"})

	ctx := store.StoreContext{StoreID: 1}

	suite.mgr, err = NewInventoryMgr(&ctx)
	assert.Nil(suite.T(), err)

	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-DEFAULT"}
	suite.itemResp, err = suite.mgr.CreateStockItem(payload)
	assert.Nil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreation() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-CREATION"}
	resp, err := suite.mgr.CreateStockItem(payload)
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), "TEST-CREATION", resp.SKU)
	}
}

func (suite *InventoryManagerTestSuite) TestFindByID() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-FIND"}
	resp, err := suite.mgr.CreateStockItem(payload)
	if assert.Nil(suite.T(), err) {
		item, err := suite.mgr.FindStockItemByID(resp.ID)
		if assert.Nil(suite.T(), err) {
			assert.Equal(suite.T(), "TEST-FIND", item.SKU)
		}
	}
}

func (suite *InventoryManagerTestSuite) TestEmptySKU() {
	payload := &payloads.StockItem{StockLocationID: 1}
	_, err := suite.mgr.CreateStockItem(payload)
	assert.NotNil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreateStockItemsUnits() {
	payload := &payloads.StockItemUnits{
		StockItemID: suite.itemResp.ID,
		Qty:         1,
		UnitCost:    500,
		Status:      "available",
	}

	err := suite.mgr.IncrementStockItemUnits(payload)
	assert.Nil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreateMultipleStockItemUnits() {
	payload := &payloads.StockItemUnits{
		StockItemID: suite.itemResp.ID,
		Qty:         10,
		UnitCost:    500,
		Status:      "available",
	}

	err := suite.mgr.IncrementStockItemUnits(payload)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 10, len(units))
	}
}
