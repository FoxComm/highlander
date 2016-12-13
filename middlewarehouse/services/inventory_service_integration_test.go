package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"fmt"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type InventoryServiceIntegrationTestSuite struct {
	GeneralServiceTestSuite
	itemResp       *models.StockItem
	service        IInventoryService
	summaryService ISummaryService
	sl             *models.StockLocation
	skuId          uint
	skuCode        string
}

func TestInventoryServiceIntegrationSuite(t *testing.T) {
	suite.Run(t, new(InventoryServiceIntegrationTestSuite))
}

func (suite *InventoryServiceIntegrationTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	tasks.TruncateTables(suite.db, []string{
		"stock_locations",
	})

	summaryRepository := repositories.NewSummaryRepository(suite.db)
	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)
	stockLocationRepository := repositories.NewStockLocationRepository(suite.db)

	stockLocationService := NewStockLocationService(stockLocationRepository)

	suite.summaryService = NewSummaryService(summaryRepository, stockItemRepository)
	suite.service = &inventoryService{stockItemRepository, unitRepository, suite.summaryService, false}

	suite.sl, _ = stockLocationService.CreateLocation(fixtures.GetStockLocation())
	suite.skuId = 1
	suite.skuCode = "SKU-INTEGRATION"
}

func (suite *InventoryServiceIntegrationTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"stock_item_transactions",
		"inventory_search_view",
		"inventory_transactions_search_view",
	})
}

func (suite *InventoryServiceIntegrationTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *InventoryServiceIntegrationTestSuite) Test_CreateStockItem_SummaryCreation() {
	_, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)

	summary, err := suite.summaryService.GetSummaryBySKU(suite.skuId)

	suite.Nil(err)
	suite.Equal(4, len(summary))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_IncrementStockItemUnits_SummaryUpdate() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 5)))

	summary, err := suite.summaryService.GetSummaryBySKU(suite.skuId)

	suite.Nil(err)
	suite.Equal(5, summary[0].OnHand)
	suite.Equal(5, summary[0].AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_DecrementStockItemUnits_SummaryUpdate() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	err = suite.service.DecrementStockItemUnits(stockItem.ID, models.Sellable, 7)
	suite.Nil(err)

	summary, err := suite.summaryService.GetSummaryBySKU(suite.skuId)

	suite.Nil(err)
	suite.Equal(3, summary[0].OnHand)
	suite.Equal(3, summary[0].AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_MultipleSKUsSummary() {
	sku1 := "TEST-RESERVATION-A"
	sku2 := "TEST-RESERVATION-B"

	stockItem1, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, 1, sku1))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem1.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem1, 5)))

	stockItem2, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, 2, sku2))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem2.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem2, 5)))

	refNum := "BR10001"
	skus := map[uint]int{
		1: 3,
		2: 5,
	}

	suite.service.HoldItems(refNum, skus)

	summary1, err := suite.summaryService.GetSummaryBySKU(1)
	suite.Nil(err)

	summary2, err := suite.summaryService.GetSummaryBySKU(2)
	suite.Nil(err)

	suite.Equal(skus[1], summary1[0].OnHold)
	suite.Equal(skus[2], summary2[0].OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_SubsequentSummary() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	skus := map[uint]int{suite.skuId: 3}

	suite.Nil(suite.service.HoldItems("BR10001", skus))

	summary, err := suite.summaryService.GetSummaryBySKU(suite.skuId)
	suite.Nil(err)

	suite.Equal(skus[suite.skuId], summary[0].OnHold)

	skus[suite.skuId] = 5

	suite.Nil(suite.service.HoldItems("BR10002", skus))

	summary, err = suite.summaryService.GetSummaryBySKU(suite.skuId)
	suite.Nil(err)

	suite.Equal(8, summary[0].OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_Summary() {
	skus := map[uint]int{suite.skuId: 1}
	refNum := "BR10001"
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 1)))
	suite.Nil(suite.service.HoldItems(refNum, skus))

	suite.Nil(suite.service.ReleaseItems(refNum))

	summary, err := suite.summaryService.GetSummaryBySKU(suite.skuId)
	suite.Nil(err)

	suite.Equal(0, summary[0].OnHold, "No stock item units should be onHold")
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, err := suite.service.GetAFSByID(stockItem.ID, models.Sellable)

	suite.Nil(err)
	suite.Equal(stockItem.ID, afs.StockItemID)
	suite.Equal(stockItem.SkuID, afs.SkuID)
	suite.Equal(10, afs.AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID_NotFound() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, err := suite.service.GetAFSByID(uint(222), models.Sellable)

	suite.Equal(fmt.Errorf(repositories.ErrorStockItemNotFound, 222), err)
	suite.Nil(afs)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, _ := suite.service.GetAFSBySkuCode(stockItem.SkuCode, models.Sellable)

	suite.Equal(stockItem.SkuCode, afs.SkuCode)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU_NotFound() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.skuId, suite.skuCode))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, err := suite.service.GetAFSBySkuCode("BLA-BLA-SKU", models.Sellable)

	suite.Equal(gorm.ErrRecordNotFound, err)
	suite.Nil(afs)
}
