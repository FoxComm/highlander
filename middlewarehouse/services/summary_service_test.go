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

type SummaryManagerTestSuite struct {
	suite.Suite
	service  ISummaryService
	itemResp *models.StockItem
	db       *gorm.DB
}

func TestSummaryManagerSuite(t *testing.T) {
	suite.Run(t, new(SummaryManagerTestSuite))
}

func (suite *SummaryManagerTestSuite) SetupTest() {
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

func (suite *SummaryManagerTestSuite) TestIncrementOnHand() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, statusChange{to: "onHand"}, nil)
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.OnHand)
	}
}

func (suite *SummaryManagerTestSuite) TestIncrementOnHold() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, statusChange{to: "onHold"}, nil)
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.OnHold)
	}
}

func (suite *SummaryManagerTestSuite) TestIncrementReserved() {
	err := suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, statusChange{to: "reserved"}, nil)
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.Reserved)
	}
}

func (suite *SummaryManagerTestSuite) TestStatusTransition() {
	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 5, statusChange{to: "onHold"}, nil)

	summary := models.StockItemSummary{}
	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 0, summary.OnHand)
	assert.Equal(suite.T(), 5, summary.OnHold)
	assert.Equal(suite.T(), 0, summary.Reserved)

	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 2, statusChange{from: "onHold", to: "reserved"}, nil)

	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 0, summary.OnHand)
	assert.Equal(suite.T(), 3, summary.OnHold)
	assert.Equal(suite.T(), 2, summary.Reserved)

	suite.service.UpdateStockItemSummary(suite.itemResp.ID, 1, statusChange{from: "reserved", to: "onHand"}, nil)

	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 1, summary.OnHand)
	assert.Equal(suite.T(), 3, summary.OnHold)
	assert.Equal(suite.T(), 1, summary.Reserved)
}
