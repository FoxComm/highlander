package services

import (
	"testing"
	"time"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

const SKU = "SKU-INTEGRATION"

type InventoryServiceIntegrationTestSuite struct {
	suite.Suite
	itemResp       *models.StockItem
	service        IInventoryService
	summaryService ISummaryService
	db             *gorm.DB
	sl             *models.StockLocation
}

func TestInventoryServiceIntegrationSuite(t *testing.T) {
	suite.Run(t, new(InventoryServiceIntegrationTestSuite))
}

func (suite *InventoryServiceIntegrationTestSuite) SetupSuite() {
	suite.db, _ = config.DefaultConnection()

	summaryRepository := repositories.NewSummaryRepository(suite.db)
	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)
	stockLocationRepository := repositories.NewStockLocationRepository(suite.db)

	stockLocationService := NewStockLocationService(stockLocationRepository)

	suite.summaryService = NewSummaryService(summaryRepository, stockItemRepository)
	suite.service = NewInventoryService(stockItemRepository, unitRepository, suite.summaryService)

	suite.sl, _ = stockLocationService.CreateLocation(fixtures.GetStockLocation())
}

func (suite *InventoryServiceIntegrationTestSuite) SetupTest() {

	time.Sleep(100 * time.Millisecond)

	tasks.TruncateTables([]string{
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"stock_item_transactions",
		"inventory_search_view",
	})
}

func (suite *InventoryServiceIntegrationTestSuite) Test_CreateStockItem_SummaryCreation() {
	suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary, err := suite.summaryService.GetSummaryBySKU(SKU)

	suite.Nil(err)
	suite.Equal(4, len(summary))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_IncrementStockItemUnits_SummaryUpdate() {
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 5))

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary, err := suite.summaryService.GetSummaryBySKU(SKU)

	suite.Nil(err)
	suite.Equal(5, summary[0].OnHand)
	suite.Equal(5, summary[0].AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_DecrementStockItemUnits_SummaryUpdate() {
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 10))
	suite.service.DecrementStockItemUnits(stockItem.ID, models.Sellable, 7)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary, err := suite.summaryService.GetSummaryBySKU(SKU)

	suite.Nil(err)
	suite.Equal(3, summary[0].OnHand)
	suite.Equal(3, summary[0].AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_MultipleSKUsSummary() {
	sku1 := "TEST-RESERVATION-A"
	sku2 := "TEST-RESERVATION-B"

	stockItem1, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku1))
	suite.service.IncrementStockItemUnits(stockItem1.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem1.ID, 5))

	stockItem2, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku2))
	suite.service.IncrementStockItemUnits(stockItem2.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem2.ID, 5))

	refNum := "BR10001"
	skus := map[string]int{
		sku1: 3,
		sku2: 5,
	}

	suite.service.ReserveItems(refNum, skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary1, _ := suite.summaryService.GetSummaryBySKU(sku1)
	summary2, _ := suite.summaryService.GetSummaryBySKU(sku2)

	suite.Equal(skus[sku1], summary1[0].OnHold)
	suite.Equal(skus[sku2], summary2[0].OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_SubsequentSummary() {
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 10))

	skus := map[string]int{SKU: 3}

	suite.service.ReserveItems("BR10001", skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary, _ := suite.summaryService.GetSummaryBySKU(SKU)

	suite.Equal(skus[SKU], summary[0].OnHold)

	skus[SKU] = 5

	suite.service.ReserveItems("BR10002", skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary, _ = suite.summaryService.GetSummaryBySKU(SKU)

	suite.Equal(8, summary[0].OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_Summary() {
	skus := map[string]int{SKU: 1}
	refNum := "BR10001"
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 1))
	suite.service.ReserveItems(refNum, skus)

	suite.service.ReleaseItems(refNum)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary, _ := suite.summaryService.GetSummaryBySKU(SKU)

	suite.Equal(0, summary[0].OnHold, "No stock item units should be onHold")
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID() {
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 10))

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	afs, err := suite.service.GetAFSByID(stockItem.ID, models.Sellable)

	suite.Nil(err)
	suite.Equal(stockItem.ID, afs.StockItemID)
	suite.Equal(stockItem.SKU, afs.SKU)
	suite.Equal(10, afs.AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID_NotFound() {
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 10))

	afs, err := suite.service.GetAFSByID(uint(222), models.Sellable)

	suite.Equal(gorm.ErrRecordNotFound, err)
	suite.Nil(afs)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU() {
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 10))

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	afs, _ := suite.service.GetAFSBySKU(stockItem.SKU, models.Sellable)

	suite.Equal(stockItem.SKU, afs.SKU)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU_NotFound() {
	stockItem, _ := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, SKU))
	suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem.ID, 10))

	afs, err := suite.service.GetAFSBySKU("BLA-BLA-SKU", models.Sellable)

	suite.Equal(gorm.ErrRecordNotFound, err)
	suite.Nil(afs)
}
