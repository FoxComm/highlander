package repositories

import (
	"database/sql"
	"fmt"
	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/common/tests"
	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/suite"
)

type ShipmentLineItemRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository     IShipmentLineItemRepository
	stockItemUnit1 *models.StockItemUnit
	stockItemUnit2 *models.StockItemUnit
	shipment1      *models.Shipment
}

func TestShipmentLineItemRepositorySuite(t *testing.T) {
	suite.Run(t, new(ShipmentLineItemRepositoryTestSuite))
}

func (suite *ShipmentLineItemRepositoryTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	suite.repository = NewShipmentLineItemRepository(suite.db)

	tasks.TruncateTables([]string{
		"carriers",
		"shipping_methods",
		"addresses",
		"stock_locations",
		"stock_items",
		"stock_item_units",
		"shipments",
	})

	carrier := fixtures.GetCarrier(1)
	suite.db.Create(carrier)

	shippingMethod := fixtures.GetShippingMethod(1, carrier.ID, carrier)
	suite.db.Create(shippingMethod)

	region := &models.Region{}
	suite.db.Preload("Country").First(region)
	address := fixtures.GetAddress(1, region.ID, region)
	suite.db.Create(address)

	stockLocation := fixtures.GetStockLocation()
	suite.db.Create(stockLocation)

	stockItem := fixtures.GetStockItem(stockLocation.ID, "SKU-TEST")
	suite.db.Create(stockItem)

	suite.stockItemUnit1 = fixtures.GetStockItemUnit(stockItem)
	suite.stockItemUnit1.RefNum = sql.NullString{"BR1001", true}
	suite.db.Create(suite.stockItemUnit1)

	suite.stockItemUnit2 = fixtures.GetStockItemUnit(stockItem)
	suite.stockItemUnit2.RefNum = sql.NullString{"BR1001", true}
	suite.db.Create(suite.stockItemUnit2)

	suite.shipment1 = fixtures.GetShipment(0, shippingMethod.ID, shippingMethod, address.ID, address, nil)
	suite.db.Create(suite.shipment1)
}

func (suite *ShipmentLineItemRepositoryTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
		"shipment_line_items",
	})
}

func (suite *ShipmentLineItemRepositoryTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_GetShipmentLineItemsByShipmentID_Found_ReturnsShipmentLineItemModels() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(1, suite.shipment1.ID, suite.stockItemUnit1.ID)
	suite.db.Create(shipmentLineItem1)
	shipmentLineItem2 := fixtures.GetShipmentLineItem(2, suite.shipment1.ID, suite.stockItemUnit1.ID)
	suite.db.Create(shipmentLineItem2)

	//act
	shipmentLineItems, err := suite.repository.GetShipmentLineItemsByShipmentID(shipmentLineItem1.ShipmentID)

	//assert
	suite.Nil(err)
	suite.Equal(2, len(shipmentLineItems))
	tests.SyncDates(shipmentLineItem1, shipmentLineItem2, shipmentLineItems[0], shipmentLineItems[1])
	suite.Equal(shipmentLineItem1, shipmentLineItems[0])
	suite.Equal(shipmentLineItem2, shipmentLineItems[1])
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_CreateShipmentLineItem_ReturnsCreatedRecord() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(1, suite.shipment1.ID, suite.stockItemUnit1.ID)

	//act
	shipmentLineItem, err := suite.repository.CreateShipmentLineItem(fixtures.GetShipmentLineItem(0, suite.shipment1.ID, suite.stockItemUnit1.ID))

	//assert
	suite.Nil(err)
	tests.SyncDates(shipmentLineItem1, shipmentLineItem)
	suite.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_UpdateShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(1, suite.shipment1.ID, suite.stockItemUnit1.ID)

	//act
	_, err := suite.repository.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.Equal(fmt.Errorf(ErrorShipmentLineItemNotFound, shipmentLineItem1.ID), err)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_UpdateShipmentLineItem_Found_ReturnsUpdatedRecord() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(1, suite.shipment1.ID, suite.stockItemUnit1.ID)
	suite.db.Create(shipmentLineItem1)
	shipmentLineItem1.Price = 4900

	//act
	shipmentLineItem, err := suite.repository.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.Nil(err)
	tests.SyncDates(shipmentLineItem1, shipmentLineItem)
	suite.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_DeleteShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//act
	err := suite.repository.DeleteShipmentLineItem(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorShipmentLineItemNotFound, 1), err)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_DeleteShipmentLineItem_Found_ReturnsNoError() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(1, suite.shipment1.ID, suite.stockItemUnit1.ID)
	suite.db.Create(shipmentLineItem1)

	//act
	err := suite.repository.DeleteShipmentLineItem(shipmentLineItem1.ID)

	//assert
	suite.Nil(err)
}
