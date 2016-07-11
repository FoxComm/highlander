package services

import (
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
	suite.Run(t, new(InventoryManagerTestSuite))
}

// Just a few helper functions!
func (suite *InventoryManagerTestSuite) createStockItem(sku string, qty int) (*responses.StockItem, error) {
	siPayload := &payloads.StockItem{StockLocationID: 1, SKU: sku}
	resp, err := suite.invMgr.CreateStockItem(siPayload)
	if err != nil {
		return nil, err
	}

	if qty > 0 {
		iPayload := &payloads.IncrementStockItemUnits{
			Qty:      qty,
			UnitCost: 500,
			Status:   "onHand",
		}

		err := suite.invMgr.IncrementStockItemUnits(resp.ID, iPayload)
		if err != nil {
			return nil, err
		}
	}

	return resp, nil
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

	suite.itemResp, err = suite.createStockItem("TEST-DEFAULT", 0)
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
	resp, err := suite.createStockItem("TEST-CREATION", 0)
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
	resp, err := suite.createStockItem("TEST-INCREMENT", 1)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 1, len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestCreateMultipleStockItemUnits() {
	resp, err := suite.createStockItem("TEST-INCREMENT", 10)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 10, len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestDecrementStockItemUnits() {
	resp, err := suite.createStockItem("TEST-DECREMENT", 10)
	assert.Nil(suite.T(), err)

	payload := payloads.DecrementStockItemUnits{Qty: 7}
	err = suite.invMgr.DecrementStockItemUnits(resp.ID, &payload)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 3, len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestSingleSKUReservation() {
	_, err := suite.createStockItem("TEST-RESERVATION", 1)
	assert.Nil(suite.T(), err)

	resPayload := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION", Qty: 1},
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

func (suite *InventoryManagerTestSuite) TestMultipleSKUReservation() {
	resp1, err := suite.createStockItem("TEST-RESERVATION-A", 5)
	assert.Nil(suite.T(), err)

	resp2, err := suite.createStockItem("TEST-RESERVATION-B", 5)
	assert.Nil(suite.T(), err)

	resPayload := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION-A", Qty: 5},
			payloads.SKUReservation{SKU: "TEST-RESERVATION-B", Qty: 5},
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
	err = suite.db.
		Where("reservation_id = ?", reservation.ID).
		Where("stock_item_id = ?", resp1.ID).
		Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, len(units))
	}

	err = suite.db.
		Where("reservation_id = ?", reservation.ID).
		Where("stock_item_id = ?", resp2.ID).
		Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestMultipleSKUQtyReservation() {
	_, err := suite.createStockItem("TEST-RESERVATION", 10)
	assert.Nil(suite.T(), err)

	resPayload := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION", Qty: 5},
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
		assert.Equal(suite.T(), 5, len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestSKUReservationNoOnHand() {
	resPayload := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-DEFAULT", Qty: 1},
		},
	}

	err := suite.invMgr.ReserveItems(resPayload)
	assert.NotNil(suite.T(), err)
}
