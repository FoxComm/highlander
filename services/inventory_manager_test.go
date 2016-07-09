package services

import (
	"fmt"
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type InventoryManagerTestSuite struct {
	suite.Suite
	itemResp *responses.StockItem
	invMgr   InventoryManager
	db       *gorm.DB
}

func TestInventoryManagerSuite(t *testing.T) {
	fmt.Println("HERE????")
	suite.Run(t, new(InventoryManagerTestSuite))
}

func (suite *InventoryManagerTestSuite) SetupTest() {
	var err error
	suite.invMgr, err = MakeInventoryManager()
	assert.Nil(suite.T(), err)

	suite.db, err = config.DefaultConnection()
	assert.Nil(suite.T(), err)

	assert.Nil(suite.T(), err)
	tasks.TruncateTables([]string{
		"reservations",
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
	})

	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-DEFAULT"}
	suite.itemResp, err = suite.invMgr.CreateStockItem(payload)
	assert.Nil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreation() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-CREATION"}
	resp, err := suite.invMgr.CreateStockItem(payload)
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), "TEST-CREATION", resp.SKU)
	}
}

func (suite *InventoryManagerTestSuite) TestSummaryCreation() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-CREATION"}
	resp, err := suite.invMgr.CreateStockItem(payload)
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, resp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 0, summary.OnHand)
		assert.Equal(suite.T(), 0, summary.OnHold)
		assert.Equal(suite.T(), 0, summary.Reserved)
	}
}

func (suite *InventoryManagerTestSuite) TestFindByID() {
	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-FIND"}
	resp, err := suite.invMgr.CreateStockItem(payload)
	if assert.Nil(suite.T(), err) {
		item, err := suite.invMgr.FindStockItemByID(resp.ID)
		if assert.Nil(suite.T(), err) {
			assert.Equal(suite.T(), "TEST-FIND", item.SKU)
		}
	}
}

func (suite *InventoryManagerTestSuite) TestEmptySKU() {
	payload := &payloads.StockItem{StockLocationID: 1}
	_, err := suite.invMgr.CreateStockItem(payload)
	assert.NotNil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreateStockItemsUnits() {
	payload := &payloads.IncrementStockItemUnits{
		Qty:      1,
		UnitCost: 500,
		Status:   "onHand",
	}

	err := suite.invMgr.IncrementStockItemUnits(suite.itemResp.ID, payload)
	assert.Nil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestCreateMultipleStockItemUnits() {
	payload := &payloads.IncrementStockItemUnits{
		Qty:      10,
		UnitCost: 500,
		Status:   "onHand",
	}

	err := suite.invMgr.IncrementStockItemUnits(suite.itemResp.ID, payload)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 10, len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestDecrementStockItemUnits() {
	for i := 0; i < 10; i += 1 {
		unit := models.StockItemUnit{
			StockItemID: suite.itemResp.ID,
			UnitCost:    500,
			Status:      "onHand",
		}

		err := suite.db.Create(&unit).Error
		if !assert.Nil(suite.T(), err) {
			return
		}
	}

	payload := payloads.DecrementStockItemUnits{Qty: 7}
	err := suite.invMgr.DecrementStockItemUnits(suite.itemResp.ID, &payload)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 3, len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestSingleSKUReservation() {
	payload := &payloads.IncrementStockItemUnits{
		Qty:      10,
		UnitCost: 500,
		Status:   "onHand",
	}

	err := suite.invMgr.IncrementStockItemUnits(suite.itemResp.ID, payload)
	assert.Nil(suite.T(), err)

	resPayload := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-DEFAULT", Qty: 1},
		},
	}

	err = suite.invMgr.ReserveItems(resPayload)
	assert.Nil(suite.T(), err)

	var reservation models.Reservation
	err = suite.db.First(&reservation).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), "BR10001", reservation.RefNum)
	}

	var units []models.StockItemUnit
	err = suite.db.Where("reservation_id = ?", reservation.ID).Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 1, len(units))
	}
}
