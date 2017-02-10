package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/stretchr/testify/suite"
)

type InventoryServiceTestSuite struct {
	GeneralServiceTestSuite
	sl      *models.StockLocation
	service InventoryService
}

func TestInventoryServiceSuite(t *testing.T) {
	suite.Run(t, new(InventoryServiceTestSuite))
}

func (suite *InventoryServiceTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	tasks.TruncateTables(suite.db, []string{
		"stock_locations",
	})

	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)

	summaryService := NewSummaryService(suite.db)
	stockLocationService := NewStockLocationService(suite.db)
	suite.service = &inventoryService{stockItemRepository, unitRepository, summaryService, suite.db, nil}

	suite.sl, _ = stockLocationService.CreateLocation(fixtures.GetStockLocation())
}

func (suite *InventoryServiceTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"skus",
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"stock_item_transactions",
		"inventory_search_view",
		"inventory_transactions_search_view",
	})
}

func (suite *InventoryServiceTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *InventoryServiceTestSuite) Test_CreateStockItem() {
	stockItem := fixtures.GetStockItem(suite.sl.ID, "SKU")

	resp, err := suite.service.CreateStockItem(stockItem)

	suite.Nil(err)
	suite.Equal(stockItem.SKU, resp.SKU)
}

func (suite *InventoryServiceTestSuite) Test_GetStockItemById() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, "SKU"))

	resp, err := suite.service.GetStockItemById(stockItem.ID)

	suite.Nil(err)
	suite.Equal(stockItem.SKU, resp.SKU)
}

func (suite *InventoryServiceTestSuite) Test_CreateExistingStockItem() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, "SKU"))
	suite.Nil(err)

	resp, err := suite.service.GetStockItemById(stockItem.ID)

	suite.Nil(err)
	suite.Equal(stockItem.DefaultUnitCost, resp.DefaultUnitCost)

	stockItemUpdate := stockItem
	stockItemUpdate.DefaultUnitCost = 599

	respUpdate, err := suite.service.CreateStockItem(stockItemUpdate)

	suite.Nil(err)
	suite.Equal(resp.ID, respUpdate.ID)
	suite.Equal(resp.SKU, respUpdate.SKU)

	itemUpdate, err := suite.service.GetStockItemById(stockItem.ID)

	suite.Nil(err)
	suite.Equal(stockItemUpdate.DefaultUnitCost, itemUpdate.DefaultUnitCost)
}

func (suite *InventoryServiceTestSuite) Test_IncrementStockItemUnits() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, "TEST-INCREMENT"))
	suite.Nil(err)

	units := fixtures.GetStockItemUnits(stockItem, 1)

	err = suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, units)

	var resp []models.StockItemUnit
	suite.db.Where("stock_item_id = ?", stockItem.ID).Find(&resp)

	suite.Nil(err)
	suite.Equal(1, len(resp))
}

func (suite *InventoryServiceTestSuite) Test_IncrementStockItemUnits_MultipleItems() {
	unitsCount := 10
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, "TEST-INCREMENT"))
	suite.Nil(err)

	units := fixtures.GetStockItemUnits(stockItem, unitsCount)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, units))

	var resp []models.StockItemUnit
	suite.db.Where("stock_item_id = ?", stockItem.ID).Find(&resp)

	suite.Equal(unitsCount, len(units))
}

func (suite *InventoryServiceTestSuite) Test_DecrementStockItemUnits() {
	unitsCount := 10
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, "TEST-DECREMENT"))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, unitsCount)))

	err = suite.service.DecrementStockItemUnits(stockItem.ID, models.Sellable, 7)

	var units []models.StockItemUnit
	suite.db.Where("stock_item_id = ?", stockItem.ID).Find(&units)

	suite.Nil(err)
	suite.Equal(3, len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_SingleSKU() {
	sku := suite.createSKU("TEST-RESERVATION")
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku.Code))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 1)))

	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: sku.Code, Qty: 1},
		},
	}

	err = suite.service.HoldItems(payload)

	var units []models.StockItemUnit
	suite.db.Where("ref_num = ?", payload.RefNum).Find(&units)

	suite.Nil(err)
	suite.Equal(1, len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_MultipleSKUs() {
	sku1 := suite.createSKU("TEST-RESERVATION-A")
	sku2 := suite.createSKU("TEST-RESERVATION-B")

	sl := []models.StockLocation{}
	suite.db.Find(&sl)

	stockItem1, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku1.Code))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem1.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem1, 5)))

	stockItem2, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku2.Code))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem2.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem2, 5)))

	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: sku1.Code, Qty: 5},
			payloads.ItemReservation{SKU: sku2.Code, Qty: 5},
		},
	}

	suite.Nil(suite.service.HoldItems(payload))

	var units []models.StockItemUnit
	suite.Nil(suite.db.Where("ref_num = ?", payload.RefNum).Where("stock_item_id = ?", stockItem1.ID).Find(&units).Error)

	suite.Equal(5, len(units))

	suite.Nil(suite.db.Where("ref_num = ?", payload.RefNum).Where("stock_item_id = ?", stockItem2.ID).Find(&units).Error)

	suite.Equal(5, len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_NoOnHand() {
	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: "TEST-DEFAULT", Qty: 1},
		},
	}

	err := suite.service.HoldItems(payload)
	suite.NotNil(err)
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_NoSKU() {
	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: "NO-SKU", Qty: 1},
		},
	}

	err := suite.service.HoldItems(payload)
	suite.NotNil(err)
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_NoReservedSKUs() {
	_, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, "SKU"))
	suite.Nil(err)

	err = suite.service.ReleaseItems("BR10001")
	suite.NotNil(err, "Should not be able to unreserve items while there are no reservations")
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_Single() {
	sku := suite.createSKU("TEST-UNRESERVATION-A")

	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: sku.Code, Qty: 1},
		},
	}
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku.Code))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 1)))

	err = suite.service.HoldItems(payload)

	onHoldUnitsCount := 0
	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", payload.RefNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.Equal(1, onHoldUnitsCount, "There should be one unit in onHold status")

	// send release request and check if it was processed successfully
	err = suite.service.ReleaseItems(payload.RefNum)
	suite.Nil(err, "Reservation should be successfully removed")

	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", payload.RefNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.Equal(0, onHoldUnitsCount, "There should not be units in onHold status")
}

func (suite *InventoryServiceTestSuite) createSKU(code string) *models.SKU {
	sku := fixtures.GetSKU()
	sku.Code = code
	sku.RequiresInventoryTracking = true
	suite.Nil(suite.db.Create(sku).Error)
	return sku
}
