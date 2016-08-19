package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/stretchr/testify/suite"
)

type ShipmentServiceTestSuite struct {
	GeneralServiceTestSuite
	service IShipmentService
}

func TestShipmentServiceSuite(t *testing.T) {
	suite.Run(t, new(ShipmentServiceTestSuite))
}

func (suite *ShipmentServiceTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
		"addresses",
		"carriers",
		"shipping_methods",
		"shipments",
		"shipment_line_items",
		"stock_items",
		"stock_item_units",
		"stock_locations",
	})

	var err error
	db, err := config.DefaultConnection()
	suite.db = db.Debug()
	suite.Nil(err)
	suite.service = NewShipmentService(
		suite.db,
		repositories.NewShipmentRepository(suite.db),
		repositories.NewShipmentLineItemRepository(suite.db),
		repositories.NewStockItemUnitRepository(suite.db),
	)
}

func (suite *ShipmentServiceTestSuite) Test_GetShipmentsByReferenceNumber_ReturnsShipmentModels() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	shipment2 := fixtures.GetShipmentShort(uint(2))

	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(&shipment1.Address).Error)
	suite.Nil(suite.db.Create(&shipment1.ShippingMethod.Carrier).Error)
	suite.Nil(suite.db.Create(&shipment1.ShippingMethod).Error)
	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(shipment1).Error)
	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(shipment2).Error)

	//act
	shipments, err := suite.service.GetShipmentsByReferenceNumber(shipment1.ReferenceNumber)

	//assert
	suite.Nil(err)
	suite.Equal(2, len(shipments))
	suite.Equal(shipment1.ID, shipments[0].ID)
	suite.Equal(shipment2.ID, shipments[1].ID)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_Succeed_ReturnsCreatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(0))

	carrier := fixtures.GetCarrier(0)
	suite.Nil(suite.db.Create(carrier).Error)

	method := fixtures.GetShippingMethod(0, carrier.ID, carrier)
	suite.Nil(suite.db.Create(method).Error)
	shipment1.ShippingMethodID = method.ID

	stockLocation := fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(stockLocation).Error)

	stockItem := fixtures.GetStockItem(stockLocation.ID, shipment1.ShipmentLineItems[0].SKU)
	suite.Nil(suite.db.Create(stockItem).Error)

	stockItemUnit1 := fixtures.GetStockItemUnit(0, stockItem)
	stockItemUnit1.RefNum = models.NewSqlNullStringFromString(&shipment1.ReferenceNumber)
	stockItemUnit2 := fixtures.GetStockItemUnit(0, stockItem)
	stockItemUnit2.RefNum = models.NewSqlNullStringFromString(&shipment1.ReferenceNumber)
	suite.Nil(suite.db.Create(stockItemUnit1).Error)
	suite.Nil(suite.db.Create(stockItemUnit2).Error)

	//act
	shipment, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1, shipment)
}

//func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
//	//arrange
//	shipment1 := fixtures.GetShipmentShort(uint(1))
//
//	suite.shipmentRepository.On("GetShipmentByID", shipment1.ID).Return(nil, gorm.ErrRecordNotFound).Once()
//
//	//act
//	_, err := suite.service.UpdateShipment(shipment1)
//
//	//assert
//	suite.Equal(gorm.ErrRecordNotFound, err)
//}
//
//func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_Found_ReturnsUpdatedRecord() {
//	//arrange
//	shipment1 := fixtures.GetShipmentShort(uint(1))
//
//	suite.shipmentRepository.On("GetShipmentByID", shipment1.ID).Return(shipment1, nil).Once()
//	suite.shipmentRepository.On("UpdateShipment", shipment1).Return(shipment1, nil).Once()
//
//	//act
//	shipment, err := suite.service.UpdateShipment(shipment1)
//
//	//assert
//	suite.Nil(err)
//	suite.Equal(shipment1, shipment)
//}
