package services

import (
	"testing"

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
	service IInventoryService
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
	suite.service = &inventoryService{stockItemRepository, unitRepository, summaryService, nil}

	suite.sl, _ = stockLocationService.CreateLocation(fixtures.GetStockLocation())
}

func (suite *InventoryServiceTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
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
	sku := "TEST-RESERVATION"
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 1)))

	refNum := "BR10001"
	skus := map[string]int{sku: 1}

	err = suite.service.HoldItems(refNum, skus)

	var units []models.StockItemUnit
	suite.db.Where("ref_num = ?", refNum).Find(&units)

	suite.Nil(err)
	suite.Equal(1, len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_MultipleSKUs() {
	sku1 := "TEST-RESERVATION-A"
	sku2 := "TEST-RESERVATION-B"

	sl := []models.StockLocation{}
	suite.db.Find(&sl)

	stockItem1, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku1))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem1.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem1, 5)))

	stockItem2, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku2))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem2.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem2, 5)))

	refNum := "BR10001"
	skus := map[string]int{
		sku1: 5,
		sku2: 5,
	}

	suite.Nil(suite.service.HoldItems(refNum, skus))

	var units []models.StockItemUnit
	suite.Nil(suite.db.Where("ref_num = ?", refNum).Where("stock_item_id = ?", stockItem1.ID).Find(&units).Error)

	suite.Equal(5, len(units))

	suite.Nil(suite.db.Where("ref_num = ?", refNum).Where("stock_item_id = ?", stockItem2.ID).Find(&units).Error)

	suite.Equal(skus[sku1], len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_NoOnHand() {
	refNum := "BR10001"
	skus := map[string]int{"TEST-DEFAULT": 1}

	err := suite.service.HoldItems(refNum, skus)
	suite.NotNil(err)
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_NoSKU() {
	refNum := "BR10001"
	skus := map[string]int{"NO-SKU": 1}

	err := suite.service.HoldItems(refNum, skus)
	suite.NotNil(err)
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_NoReservedSKUs() {
	_, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, "SKU"))
	suite.Nil(err)

	err = suite.service.ReleaseItems("BR10001")
	suite.NotNil(err, "Should not be able to unreserve items while there are no reservations")
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_Single() {
	sku := "TEST-UNRESERVATION-A"
	skus := map[string]int{sku: 1}
	refNum := "BR10001"
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 1)))

	err = suite.service.HoldItems(refNum, skus)

	onHoldUnitsCount := 0
	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", refNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.Equal(1, onHoldUnitsCount, "There should be one unit in onHold status")

	// send release request and check if it was processed successfully
	err = suite.service.ReleaseItems(refNum)
	suite.Nil(err, "Reservation should be successfully removed")

	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", refNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.Equal(0, onHoldUnitsCount, "There should not be units in onHold status")
}
