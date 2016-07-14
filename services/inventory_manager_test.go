package services

import (
	"testing"
	"math/rand"

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

func (suite *InventoryManagerTestSuite) createReservation(skus []string, qty int, refNum string) error  {
	r := rand.New(rand.NewSource(99))

	for _, sku := range skus {
		_, err := suite.createStockItem(sku, qty + r.Intn(10))

		if err != nil {
			return err
		}

		err = suite.invMgr.ReserveItems(payloads.Reservation{
			RefNum: refNum,
			SKUs: []payloads.SKUReservation{
				{SKU: sku, Qty: uint(qty)},
			},
		})

		if err != nil {
			return err
		}
	}

	return nil
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
	resp, err := suite.createStockItem("TEST-RESERVATION", 1)
	assert.Nil(suite.T(), err)

	refNum := "BR10001"
	reqPayload := payloads.Reservation{
		RefNum: refNum,
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION", Qty: 1},
		},
	}

	err = suite.invMgr.ReserveItems(reqPayload)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Where("ref_num = ?", refNum).Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 1, len(units))
	}

	// check if StockItemSummary.Reserved got updated for updated StockItem

	var summary models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	assert.Equal(suite.T(), len(reqPayload.SKUs), summary.OnHold)
}

func (suite *InventoryManagerTestSuite) TestMultipleSKUReservation() {
	resp1, err := suite.createStockItem("TEST-RESERVATION-A", 5)
	assert.Nil(suite.T(), err)

	resp2, err := suite.createStockItem("TEST-RESERVATION-B", 5)
	assert.Nil(suite.T(), err)

	refNum := "BR10001"
	reqPayload := payloads.Reservation{
		RefNum: refNum,
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION-A", Qty: 5},
			payloads.SKUReservation{SKU: "TEST-RESERVATION-B", Qty: 5},
		},
	}

	err = suite.invMgr.ReserveItems(reqPayload)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.
		Where("ref_num = ?", refNum).
		Where("stock_item_id = ?", resp1.ID).
		Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, len(units))
	}

	err = suite.db.
		Where("ref_num = ?", refNum).
		Where("stock_item_id = ?", resp2.ID).
		Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), int(reqPayload.SKUs[0].Qty), len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestMultipleSKUQtyReservation() {
	_, err := suite.createStockItem("TEST-RESERVATION", 10)
	assert.Nil(suite.T(), err)

	refNum := "BR10001"
	reqPayload := payloads.Reservation{
		RefNum: refNum,
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION", Qty: 5},
		},
	}

	err = suite.invMgr.ReserveItems(reqPayload)
	assert.Nil(suite.T(), err)

	var units []models.StockItemUnit
	err = suite.db.Where("ref_num = ?", refNum).Find(&units).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), int(reqPayload.SKUs[0].Qty), len(units))
	}
}

func (suite *InventoryManagerTestSuite) TestSKUReservationNoOnHand() {
	reqPayload := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-DEFAULT", Qty: 1},
		},
	}

	err := suite.invMgr.ReserveItems(reqPayload)
	assert.NotNil(suite.T(), err)
}

func (suite *InventoryManagerTestSuite) TestMultipleSKUReservationSummary() {
	resp1, _ := suite.createStockItem("TEST-RESERVATION-A", 5)
	resp2, _ := suite.createStockItem("TEST-RESERVATION-B", 5)

	reqPayload := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION-A", Qty: 3},
			payloads.SKUReservation{SKU: "TEST-RESERVATION-B", Qty: 5},
		},
	}

	suite.invMgr.ReserveItems(reqPayload)

	var summary1 models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp1.ID).First(&summary1)
	assert.Equal(suite.T(), int(reqPayload.SKUs[0].Qty), summary1.OnHold)

	var summary2 models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp2.ID).First(&summary2)
	assert.Equal(suite.T(), int(reqPayload.SKUs[1].Qty), summary2.OnHold)
}

func (suite *InventoryManagerTestSuite) TestSubsequentSKUReservationSummary() {
	resp, _ := suite.createStockItem("TEST-RESERVATION-A", 10)

	reqPayload1 := payloads.Reservation{
		RefNum: "BR10001",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION-A", Qty: 3},
		},
	}

	suite.invMgr.ReserveItems(reqPayload1)

	var summary models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	assert.Equal(suite.T(), int(reqPayload1.SKUs[0].Qty), summary.OnHold)


	reqPayload2 := payloads.Reservation{
		RefNum: "BR10002",
		SKUs: []payloads.SKUReservation{
			payloads.SKUReservation{SKU: "TEST-RESERVATION-A", Qty: 5},
		},
	}

	suite.invMgr.ReserveItems(reqPayload2)

	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	assert.Equal(suite.T(), 8, summary.OnHold)
}

func (suite *InventoryManagerTestSuite) TestNoReservedSKUsRelease() {
	suite.createStockItem("TEST-RESERVATION-A", 1)

	reqPayload := payloads.Release{RefNum: "BR10001"}

	err := suite.invMgr.ReleaseItems(reqPayload)
	assert.NotNil(suite.T(), err, "Should not be able to unreserve items while there are no reservations")
}

func (suite *InventoryManagerTestSuite) TestSingleSKURelease() {
	skus := []string{"TEST-UNRESERVATION-A"}
	refNum := "BR10001"
	err := suite.createReservation(skus, 1, refNum)
	assert.Nil(suite.T(), err)

	// check we have active reservations
	reservedUnitsCount := 0
	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Count(&reservedUnitsCount)
	assert.Equal(suite.T(), 1, reservedUnitsCount, "There should be reserved items")

	onHoldUnitsCount := 0
	suite.db.Model(&models.StockItemUnit{}).Where("status = ?", "onHold").Count(&onHoldUnitsCount)
	assert.Equal(suite.T(), 1, onHoldUnitsCount, "There should be one unit in onHold status")

	// send release request and check if it was processed successfully
	reqPayload := payloads.Release{RefNum: refNum}

	err = suite.invMgr.ReleaseItems(reqPayload)
	assert.Nil(suite.T(), err, "Reservation should be successfully removed")

	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Count(&reservedUnitsCount)
	assert.Equal(suite.T(), 0, reservedUnitsCount, "There should not be reserved units")

	suite.db.Model(&models.StockItemUnit{}).Where("status = ?", "onHold").Count(&onHoldUnitsCount)
	assert.Equal(suite.T(), 0, onHoldUnitsCount, "There should not be units in onHold status")
}