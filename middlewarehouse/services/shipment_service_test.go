package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/utils"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/services/mocks"

	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/stretchr/testify/suite"
)

type ShipmentServiceTestSuite struct {
	GeneralServiceTestSuite
	service IShipmentService
}

func TestShipmentServiceSuite(t *testing.T) {
	suite.Run(t, new(ShipmentServiceTestSuite))
}

func (suite *ShipmentServiceTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	suite.service = NewShipmentService(suite.db, &mocks.SummaryServiceStub{}, &mocks.ActivityLoggerMock{})
}

func (suite *ShipmentServiceTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"addresses",
		"carriers",
		"shipping_methods",
		"shipments",
		"shipment_line_items",
		"stock_items",
		"stock_item_units",
		"stock_locations",
	})
}

func (suite *ShipmentServiceTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *ShipmentServiceTestSuite) Test_GetShipmentsByOrderRefNum_ReturnsShipmentModels() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	shipment1.ReferenceNumber = "FS10004"
	shipment2 := fixtures.GetShipmentShort(uint(2))
	shipment1.ReferenceNumber = "FS10005"

	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(&shipment1.Address).Error)
	suite.Nil(suite.db.Create(&shipment1.ShippingMethod.Carrier).Error)
	suite.Nil(suite.db.Create(&shipment1.ShippingMethod).Error)
	shipment1.AddressID = shipment1.Address.ID
	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(shipment1).Error)

	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(&shipment2.Address).Error)
	shipment2.AddressID = shipment2.Address.ID
	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(shipment2).Error)

	//act
	shipments, err := suite.service.GetShipmentsByOrder(shipment1.OrderRefNum)

	//assert
	suite.Nil(err)
	suite.Equal(2, len(shipments))
	suite.Equal(shipment1.ID, shipments[0].ID)
	suite.Equal(shipment2.ID, shipments[1].ID)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_Succeed_ReturnsCreatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(0))
	shipment1.ShipmentLineItems[0].ID = 0
	shipment1.ShipmentLineItems[1].ID = 0

	carrier := fixtures.GetCarrier(0)
	suite.Nil(suite.db.Create(carrier).Error)

	method := fixtures.GetShippingMethod(0, carrier.ID, carrier)
	suite.Nil(suite.db.Create(method).Error)
	shipment1.ShippingMethodCode = method.Code

	stockLocation := fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(stockLocation).Error)

	stockItem := fixtures.GetStockItem(stockLocation.ID, shipment1.ShipmentLineItems[0].SKU)
	suite.Nil(suite.db.Create(stockItem).Error)

	stockItemUnit1 := fixtures.GetStockItemUnit(stockItem)
	stockItemUnit1.RefNum = utils.MakeSqlNullString(&shipment1.OrderRefNum)
	stockItemUnit1.Status = "onHold"
	stockItemUnit2 := fixtures.GetStockItemUnit(stockItem)
	stockItemUnit2.RefNum = utils.MakeSqlNullString(&shipment1.OrderRefNum)
	stockItemUnit2.Status = "onHold"
	suite.Nil(suite.db.Create(stockItemUnit1).Error)
	suite.Nil(suite.db.Create(stockItemUnit2).Error)

	//act
	shipment, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1.ShippingMethodCode, shipment.ShippingMethodCode)
	suite.Equal(shipment1.OrderRefNum, shipment.OrderRefNum)
	suite.Equal(shipment1.State, shipment.State)
}

func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_Partial_ReturnsUpdatedRecord() {
	//arrange
	shipment := fixtures.GetShipmentShort(uint(1))

	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(&shipment.Address).Error)
	suite.Nil(suite.db.Create(&shipment.ShippingMethod.Carrier).Error)
	suite.Nil(suite.db.Create(&shipment.ShippingMethod).Error)
	shipment.AddressID = shipment.Address.ID
	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(shipment).Error)

	payload := payloads.UpdateShipment{State: "shipped"}
	updateShipment := models.NewShipmentFromUpdatePayload(&payload)
	updateShipment.ID = shipment.ID

	//act
	updated, err := suite.service.UpdateShipment(updateShipment)

	//assert
	suite.Nil(err)
	suite.Equal(shipment.ID, updated.ID)
	suite.Equal(shipment.OrderRefNum, updated.OrderRefNum)
	suite.Equal(models.ShipmentStateShipped, updated.State)
}
