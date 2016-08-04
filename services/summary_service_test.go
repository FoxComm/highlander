package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/FoxComm/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type summaryServiceTestSuite struct {
	suite.Suite
	service          ISummaryService
	inventoryService IInventoryService
	si               *models.StockItem
	unitCost         int
	onHand           int
	typeId           uint
	db               *gorm.DB
	assert           *assert.Assertions
}

func TestSummaryServiceSuite(t *testing.T) {
	suite.Run(t, new(summaryServiceTestSuite))
}

func (suite *summaryServiceTestSuite) SetupSuite() {
	suite.db, _ = config.DefaultConnection()

	summaryRepository := repositories.NewSummaryRepository(suite.db)
	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)
	txnr := repositories.NewDBTransactioner(suite.db)

	suite.service = NewSummaryService(summaryRepository, stockItemRepository, txnr)
	suite.inventoryService = NewInventoryService(stockItemRepository, unitRepository, suite.service, txnr)
	suite.assert = assert.New(suite.T())
	suite.typeId = models.StockItemTypes().Sellable
	suite.onHand = 10
	suite.unitCost = 5000
}

func (suite *summaryServiceTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
		"stock_items",
		"stock_item_summaries",
	})

	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-DEFAULT", DefaultUnitCost: suite.unitCost}
	suite.si, _ = suite.inventoryService.CreateStockItem(stockItem)

	units := []*models.StockItemUnit{}
	typeId := models.StockItemTypes().Sellable

	for i := 0; i < suite.onHand; i++ {
		item := &models.StockItemUnit{
			StockItemID: stockItem.ID,
			UnitCost:    500,
			TypeID:      typeId,
			Status:      "onHand",
		}
		units = append(units, item)
	}

	suite.inventoryService.IncrementStockItemUnits(suite.si.ID, typeId, units)
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHand() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 5, models.StatusChange{To: "onHand"})
	suite.assert.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.si.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(suite.onHand+5, summary.OnHand)
	suite.assert.Equal(suite.onHand+5, summary.AFS)
	suite.assert.Equal((suite.onHand+5)*suite.unitCost, summary.AFSCost)
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHold() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 5, models.StatusChange{To: "onHold"})
	suite.assert.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.si.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(suite.onHand, summary.OnHand)
	suite.assert.Equal(5, summary.OnHold)
}

func (suite *summaryServiceTestSuite) Test_Increment_Reserved() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 5, models.StatusChange{To: "reserved"})
	suite.assert.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.si.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(suite.onHand, summary.OnHand)
	suite.assert.Equal(5, summary.Reserved)
}

func (suite *summaryServiceTestSuite) Test_Increment_Chain() {
	suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 5, models.StatusChange{From: "onHand", To: "onHold"})

	summary := models.StockItemSummary{}
	suite.db.First(&summary, suite.si.ID)
	suite.assert.Equal(suite.onHand, summary.OnHand)
	suite.assert.Equal(5, summary.OnHold)
	suite.assert.Equal(0, summary.Reserved)
	suite.assert.Equal(5, summary.AFS)
	suite.assert.Equal(5*suite.unitCost, summary.AFSCost)

	suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 2, models.StatusChange{From: "onHold", To: "reserved"})

	suite.db.First(&summary, suite.si.ID)
	suite.assert.Equal(suite.onHand, summary.OnHand)
	suite.assert.Equal(3, summary.OnHold)
	suite.assert.Equal(2, summary.Reserved)
	suite.assert.Equal(5, summary.AFS)
	suite.assert.Equal(5*suite.unitCost, summary.AFSCost)

	suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 1, models.StatusChange{From: "reserved", To: "onHand"})

	suite.db.First(&summary, suite.si.ID)
	suite.assert.Equal(suite.onHand, summary.OnHand)
	suite.assert.Equal(3, summary.OnHold)
	suite.assert.Equal(1, summary.Reserved)
	suite.assert.Equal(6, summary.AFS)
	suite.assert.Equal(6*suite.unitCost, summary.AFSCost)
}

func (suite *summaryServiceTestSuite) Test_GetSummary() {
	suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 5, models.StatusChange{To: "onHand"})

	summary, err := suite.service.GetSummary()
	suite.assert.Nil(err)

	suite.assert.NotNil(summary)
	suite.assert.Equal(4, len(summary))
	suite.assert.Equal(suite.si.StockLocationID, summary[0].StockLocationID)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU() {
	summary, err := suite.service.GetSummaryBySKU(suite.si.SKU)
	suite.assert.Nil(err)

	suite.assert.NotNil(summary)
	suite.assert.Equal(suite.onHand, summary[0].OnHand)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NotFoundSKU() {
	_, err := suite.service.GetSummaryBySKU("NO-SKU")
	suite.assert.NotNil(err, "There should be an error as entity should not be found")
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NonZero() {
	err := suite.service.UpdateStockItemSummary(suite.si.ID, suite.typeId, 5, models.StatusChange{To: "onHand"})
	suite.assert.Nil(err)

	summary, err := suite.service.GetSummaryBySKU(suite.si.SKU)
	suite.assert.Equal(suite.onHand+5, summary[0].OnHand)
}
