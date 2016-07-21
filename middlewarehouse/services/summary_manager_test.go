package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type SummaryManagerTestSuite struct {
	suite.Suite
	itemResp *responses.StockItem
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

	payload := &payloads.StockItem{StockLocationID: 1, SKU: "TEST-DEFAULT"}
	invMgr, err := MakeInventoryManager()
	suite.itemResp, err = invMgr.CreateStockItem(payload)
	assert.Nil(suite.T(), err)
}

func (suite *SummaryManagerTestSuite) TestIncrementOnHand() {
	err := UpdateStockItem(suite.itemResp.ID, 5, statusChange{to: "onHand"})
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.OnHand)
	}
}

func (suite *SummaryManagerTestSuite) TestIncrementOnHold() {
	err := UpdateStockItem(suite.itemResp.ID, 5, statusChange{to: "onHold"})
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.OnHold)
	}
}

func (suite *SummaryManagerTestSuite) TestIncrementReserved() {
	err := UpdateStockItem(suite.itemResp.ID, 5, statusChange{to: "reserved"})
	assert.Nil(suite.T(), err)

	summary := models.StockItemSummary{}
	err = suite.db.First(&summary, suite.itemResp.ID).Error
	if assert.Nil(suite.T(), err) {
		assert.Equal(suite.T(), 5, summary.Reserved)
	}
}

func (suite *SummaryManagerTestSuite) TestStatusTransition() {
	UpdateStockItem(suite.itemResp.ID, 5, statusChange{to: "onHold"})

	summary := models.StockItemSummary{}
	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 0, summary.OnHand)
	assert.Equal(suite.T(), 5, summary.OnHold)
	assert.Equal(suite.T(), 0, summary.Reserved)

	UpdateStockItem(suite.itemResp.ID, 2, statusChange{from: "onHold", to: "reserved"})

	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 0, summary.OnHand)
	assert.Equal(suite.T(), 3, summary.OnHold)
	assert.Equal(suite.T(), 2, summary.Reserved)

	UpdateStockItem(suite.itemResp.ID, 1, statusChange{from: "reserved", to: "onHand"})

	suite.db.First(&summary, suite.itemResp.ID)
	assert.Equal(suite.T(), 1, summary.OnHand)
	assert.Equal(suite.T(), 3, summary.OnHold)
	assert.Equal(suite.T(), 1, summary.Reserved)
}
