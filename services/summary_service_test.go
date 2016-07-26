package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type summaryServiceTestSuite struct {
	suite.Suite
	service  ISummaryService
	itemResp *models.StockItem
	db       *gorm.DB
}

func TestSummaryServiceSuite(t *testing.T) {
	suite.Run(t, new(summaryServiceTestSuite))
}

func (suite *summaryServiceTestSuite) SetupTest() {
	var err error
	suite.db, err = config.DefaultConnection()
	assert.Nil(suite.T(), err)

	assert.Nil(suite.T(), err)
	tasks.TruncateTables([]string{
		"stock_items",
		"stock_item_summaries",
	})

	suite.service = NewSummaryService(suite.db)
	inventoryService := NewInventoryService(suite.db, suite.service)

	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-DEFAULT"}
	suite.itemResp, err = inventoryService.CreateStockItem(stockItem)
	assert.Nil(suite.T(), err)
}

func (suite *summaryServiceTestSuite) TestIncrementOnHand() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "onHand"}, nil)
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.OnHand)
	}
}

func (suite *summaryServiceTestSuite) TestIncrementOnHold() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "onHold"}, nil)
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.OnHold)
	}
}

func (suite *summaryServiceTestSuite) TestIncrementReserved() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "reserved"}, nil)
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.Reserved)
	}
}

func (suite *summaryServiceTestSuite) TestStatusTransition() {
	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, StatusChange{to: "onHold"}, nil)

	summary := models.StockItemSummary{}
	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 0, summary.OnHand)
	assert.Equal(suite.T(), 5, summary.OnHold)
	assert.Equal(suite.T(), 0, summary.Reserved)

	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 2, StatusChange{from: "onHold", to: "reserved"}, nil)

	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 0, summary.OnHand)
	assert.Equal(suite.T(), 3, summary.OnHold)
	assert.Equal(suite.T(), 2, summary.Reserved)

	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 1, StatusChange{from: "reserved", to: "onHand"}, nil)

	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 1, summary.OnHand)
	assert.Equal(suite.T(), 3, summary.OnHold)
	assert.Equal(suite.T(), 1, summary.Reserved)
}

func (suite *summaryServiceTestSuite) TestGetSummaries() {
	onHandCount := 5
	suite.service.UpdateStockItemSummary(suite.itemResp.ID, onHandCount, StatusChange{to: "onHand"}, nil)

	summary, err := suite.service.GetSummaries()
	assert.Nil(suite.T(), err)

	assert.NotNil(suite.T(), summary)
	assert.Equal(suite.T(), onHandCount, summary[0].OnHand)
	assert.Equal(suite.T(), suite.itemResp.SKU, summary[0].SKU)
}

func (suite *summaryServiceTestSuite) TestGetSummaryById() {
	summary, err := suite.service.GetSummaryBySKU(suite.itemResp.SKU)
	assert.Nil(suite.T(), err)

	assert.NotNil(suite.T(), summary)
	assert.Equal(suite.T(), 0, summary.OnHand)
}

func (suite *summaryServiceTestSuite) TestSummaryByIdNotFoundSKU() {
	_, err := suite.service.GetSummaryBySKU("NO-SKU")
	assert.NotNil(suite.T(), err, "There should be an error as entity should not be found")
}

func (suite *summaryServiceTestSuite) TestGetSummaryByIdNotEmpty() {
	onHandCount := 5
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, onHandCount, StatusChange{to: "onHand"}, nil)
	assert.Nil(suite.T(), err)

	summary, err := suite.service.GetSummaryBySKU(suite.itemResp.SKU)
	assert.Equal(suite.T(), onHandCount, summary.OnHand)
}
