package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type summaryServiceTestSuite struct {
	suite.Suite
	service          SummaryService
	inventoryService IInventoryService
	si               *models.StockItem
	unitCost         int
	onHand           int
	db               *gorm.DB
	assert           *assert.Assertions
}

func TestSummaryServiceSuite(t *testing.T) {
	suite.Run(t, new(summaryServiceTestSuite))
}

func (suite *summaryServiceTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	tasks.TruncateTables(suite.db, []string{
		"stock_items",
		"stock_locations",
		"inventory_search_view",
	})

	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	stockLocationRepository := repositories.NewStockLocationRepository(suite.db)
	stockItemUnitRepository := repositories.NewStockItemUnitRepository(suite.db)

	suite.service = NewSummaryService(suite.db)

	inventoryService := NewInventoryService(stockItemRepository, stockItemUnitRepository, suite.service)
	stockLocationService := NewStockLocationService(stockLocationRepository)

	sl, _ := stockLocationService.CreateLocation(fixtures.GetStockLocation())
	si, _ := inventoryService.CreateStockItem(fixtures.GetStockItem(sl.ID, "SKU"))

	suite.si = si
	suite.onHand = 10
	suite.unitCost = 5000
}

func (suite *summaryServiceTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"stock_item_summaries",
		"stock_item_transactions",
		"inventory_search_view",
		"inventory_transactions_search_view",
	})

	// setup initial summary for all tests
	suite.service.CreateStockItemSummary(suite.si.ID)
	suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, suite.onHand, models.StatusChange{To: models.StatusOnHand})
}

func (suite *summaryServiceTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHand() {
	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHand}))

	summary := models.StockItemSummary{}
	suite.Nil(suite.db.First(&summary, suite.si.ID).Error)
	suite.Equal(suite.onHand+5, summary.OnHand)
	suite.Equal(suite.onHand+5, summary.AFS)
	suite.Equal((suite.onHand+5)*suite.unitCost, summary.AFSCost)
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHold() {
	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHold}))

	summary := models.StockItemSummary{}
	suite.Nil(suite.db.First(&summary, suite.si.ID).Error)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(5, summary.OnHold)
}

func (suite *summaryServiceTestSuite) Test_Increment_Reserved() {
	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusReserved}))

	summary := models.StockItemSummary{}
	suite.Nil(suite.db.First(&summary, suite.si.ID).Error)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(5, summary.Reserved)
}

func (suite *summaryServiceTestSuite) Test_Increment_Chain() {
	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{From: models.StatusOnHand, To: models.StatusOnHold}))

	summary := models.StockItemSummary{}
	suite.Nil(suite.db.First(&summary, suite.si.ID).Error)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(5, summary.OnHold)
	suite.Equal(0, summary.Reserved)
	suite.Equal(5, summary.AFS)
	suite.Equal(5*suite.unitCost, summary.AFSCost)

	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 2, models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved}))

	suite.Nil(suite.db.First(&summary, suite.si.ID).Error)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(3, summary.OnHold)
	suite.Equal(2, summary.Reserved)
	suite.Equal(5, summary.AFS)
	suite.Equal(5*suite.unitCost, summary.AFSCost)

	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 1, models.StatusChange{From: models.StatusReserved, To: models.StatusOnHand}))

	suite.Nil(suite.db.First(&summary, suite.si.ID).Error)
	suite.Equal(suite.onHand, summary.OnHand)
	suite.Equal(3, summary.OnHold)
	suite.Equal(1, summary.Reserved)
	suite.Equal(6, summary.AFS)
	suite.Equal(6*suite.unitCost, summary.AFSCost)

	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 1, models.StatusChange{From: models.StatusReserved, To: models.StatusShipped}))

	suite.Nil(suite.db.First(&summary, suite.si.ID).Error)
	suite.Equal(suite.onHand-1, summary.OnHand)
	suite.Equal(3, summary.OnHold)
	suite.Equal(0, summary.Reserved)
	suite.Equal(1, summary.Shipped)
	suite.Equal(6, summary.AFS)
}

func (suite *summaryServiceTestSuite) Test_GetSummary() {
	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHand}))

	summary, err := suite.service.GetSummary()
	suite.Nil(err)

	suite.NotNil(summary)
	suite.Equal(4, len(summary))
	suite.Equal(suite.si.StockLocationID, summary[0].StockItem.StockLocation.ID)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU() {
	summary, err := suite.service.GetSummaryBySKU(suite.si.SKU)
	suite.Nil(err)

	suite.NotNil(summary)
	suite.Equal(suite.onHand, summary[0].OnHand)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NotFoundSKU() {
	_, err := suite.service.GetSummaryBySKU("NO-SKU")
	suite.NotNil(err, "There should be an error as entity should not be found")
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NonZero() {
	suite.Nil(suite.service.UpdateStockItemSummary(suite.si.ID, models.Sellable, 5, models.StatusChange{To: models.StatusOnHand}))

	summary, err := suite.service.GetSummaryBySKU(suite.si.SKU)
	suite.Nil(err)
	suite.Equal(suite.onHand+5, summary[0].OnHand)
}
