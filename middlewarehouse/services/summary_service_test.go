package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type summaryServiceTestSuite struct {
	suite.Suite
	service  ISummaryService
	itemResp *models.StockItem
	db       *gorm.DB
	assert   *assert.Assertions
}

func TestSummaryServiceSuite(t *testing.T) {
	suite.Run(t, new(summaryServiceTestSuite))
}

func (suite *summaryServiceTestSuite) SetupSuite() {
	suite.db, _ = config.DefaultConnection()
	suite.service = NewSummaryService(suite.db)
	suite.assert = assert.New(suite.T())
}

func (suite *summaryServiceTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
		"stock_items",
		"stock_item_summaries",
	})

	inventoryService := NewInventoryService(suite.db, suite.service)

	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-DEFAULT"}
	suite.itemResp, _ = inventoryService.CreateStockItem(stockItem)
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHand() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "onHand"}, nil)
	suite.assert.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(5, summary.OnHand)
}

func (suite *summaryServiceTestSuite) Test_Increment_OnHold() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "onHold"}, nil)
	suite.assert.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(5, summary.OnHold)
}

func (suite *summaryServiceTestSuite) Test_Increment_Reserved() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "reserved"}, nil)
	suite.assert.Nil(err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(5, summary.Reserved)
}

func (suite *summaryServiceTestSuite) Test_Increment_Chain() {
	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "onHold"}, nil)

	summary := models.StockItemSummary{}
	suite.db.First(&summary, suite.itemResp.ID)
	suite.assert.Equal(0, summary.OnHand)
	suite.assert.Equal(5, summary.OnHold)
	suite.assert.Equal(0, summary.Reserved)

	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 2, StatusChange{from: "onHold", to: "reserved"}, nil)

	suite.db.First(&summary, suite.itemResp.ID)
	suite.assert.Equal(0, summary.OnHand)
	suite.assert.Equal(3, summary.OnHold)
	suite.assert.Equal(2, summary.Reserved)

	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 1, StatusChange{from: "reserved", to: "onHand"}, nil)

	suite.db.First(&summary, suite.itemResp.ID)
	suite.assert.Equal(1, summary.OnHand)
	suite.assert.Equal(3, summary.OnHold)
	suite.assert.Equal(1, summary.Reserved)
}

func (suite *summaryServiceTestSuite) Test_GetSummary() {
	onHandCount := 5
	suite.service.UpdateStockItemSummary(suite.itemResp.ID, onHandCount, StatusChange{to: "onHand"}, nil)

	summary, err := suite.service.GetSummary()
	suite.assert.Nil(err)

	suite.assert.NotNil(summary)
	suite.assert.Equal(onHandCount, summary[0].OnHand)
	suite.assert.Equal(suite.itemResp.SKU, summary[0].SKU)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU() {
	summary, err := suite.service.GetSummaryBySKU(suite.itemResp.SKU)
	suite.assert.Nil(err)

	suite.assert.NotNil(summary)
	suite.assert.Equal(0, summary.OnHand)
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NotFoundSKU() {
	_, err := suite.service.GetSummaryBySKU("NO-SKU")
	suite.assert.NotNil(err, "There should be an error as entity should not be found")
}

func (suite *summaryServiceTestSuite) Test_GetSummaryBySKU_NonZero() {
	onHandCount := 5
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, onHandCount, StatusChange{to: "onHand"}, nil)
	suite.assert.Nil(err)

	summary, err := suite.service.GetSummaryBySKU(suite.itemResp.SKU)
	suite.assert.Equal(onHandCount, summary.OnHand)
}
