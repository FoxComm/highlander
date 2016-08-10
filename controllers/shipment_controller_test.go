package controllers

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/controllers/mocks"

	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type shipmentControllerTestSuite struct {
	GeneralControllerTestSuite
	shipmentService         *mocks.ShipmentServiceMock
	addressService          *mocks.AddressServiceMock
	shipmentLineItemService *mocks.ShipmentLineItemServiceMock
	//shipmentTransactionService *mocks.ShipmentTransactionServiceMock
}

func TestShipmentControllerSuite(t *testing.T) {
	suite.Run(t, new(shipmentControllerTestSuite))
}

func (suite *shipmentControllerTestSuite) SetupSuite() {
	suite.router = gin.New()

	suite.shipmentService = &mocks.ShipmentServiceMock{}
	suite.addressService = &mocks.AddressServiceMock{}
	suite.shipmentLineItemService = &mocks.ShipmentLineItemServiceMock{}
	//suite.shipmentTransactionService = &mocks.ShipmentTransactionServiceMock{}

	controller := NewShipmentController(suite.shipmentService, suite.addressService, suite.shipmentLineItemService /*, suite.shipmentTransactionService*/)
	controller.SetUp(suite.router.Group("/shipments"))

	suite.assert = assert.New(suite.T())
}

func (suite *shipmentControllerTestSuite) TearDownTest() {
	// clear mock calls expectations after each test
	suite.shipmentService.AssertExpectations(suite.T())
	suite.shipmentService.ExpectedCalls = []*mock.Call{}
	suite.shipmentService.Calls = []mock.Call{}
	suite.addressService.AssertExpectations(suite.T())
	suite.addressService.ExpectedCalls = []*mock.Call{}
	suite.addressService.Calls = []mock.Call{}
	suite.shipmentLineItemService.AssertExpectations(suite.T())
	suite.shipmentLineItemService.ExpectedCalls = []*mock.Call{}
	suite.shipmentLineItemService.Calls = []mock.Call{}
	//suite.shipmentTransactionService.AssertExpectations(suite.T())
	//suite.shipmentTransactionService.ExpectedCalls = []*mock.Call{}
	//suite.shipmentTransactionService.Calls = []mock.Call{}
}

func (suite *shipmentControllerTestSuite) Test_GetShipmentsByReferenceNumbers_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.shipmentService.On("GetShipmentsByReferenceNumber", "BR1005").Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := responses.Error{}
	response := suite.Get("/shipments/BR1005", &errors)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(errors.Errors))
	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
}

func (suite *shipmentControllerTestSuite) Test_GetShipmentsByReferenceNumbers_Found_ReturnsRecords() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	shipment2 := fixtures.GetShipmentShort(uint(2))
	suite.shipmentService.On("GetShipmentsByReferenceNumber", shipment1.ReferenceNumber).Return([]*models.Shipment{
		shipment1,
	}, nil).Once()
	suite.shipmentService.On("GetShipmentsByReferenceNumber", shipment2.ReferenceNumber).Return([]*models.Shipment{
		shipment2,
	}, nil).Once()

	//act
	shipments := []*responses.Shipment{}
	response := suite.Get(fmt.Sprintf("/shipments/%s,%s", shipment1.ReferenceNumber, shipment2.ReferenceNumber), &shipments)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(2, len(shipments))
	suite.assert.Equal(responses.NewShipmentFromModel(shipment1), shipments[0])
	suite.assert.Equal(responses.NewShipmentFromModel(shipment2), shipments[1])
}

func (suite *shipmentControllerTestSuite) Test_CreateShipment_ReturnsRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	payload := fixtures.ToShipmentPayload(shipment1)
	suite.shipmentService.On("CreateShipment", models.NewShipmentFromPayload(payload)).Return(shipment1, nil).Once()

	//act
	shipment := &responses.Shipment{}
	response := suite.Post("/shipments", payload, shipment)

	//assert
	suite.assert.Equal(http.StatusCreated, response.Code)
	suite.assert.Equal(responses.NewShipmentFromModel(shipment1), shipment)
}

func (suite *shipmentControllerTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	suite.shipmentService.
		On("UpdateShipment", fixtures.GetShipment(shipment1.ID, shipment1.ShippingMethodID, &models.ShippingMethod{},
			shipment1.AddressID, &shipment1.Address, []models.ShipmentLineItem{shipment1.ShipmentLineItems[0], shipment1.ShipmentLineItems[1]})).
		Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := &responses.Error{}
	response := suite.Put("/shipments/1", fixtures.ToShipmentPayload(shipment1), errors)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(errors.Errors))
	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
}

//
//func (suite *shipmentControllerTestSuite) Test_UpdateShipment_Found_ReturnsRecord() {
//	//arrange
//	shipment1 := fixtures.GetShipmentShort(uint(1))
//	suite.shipmentService.On("UpdateShipment", shipment1).Return(shipment1, nil).Once()
//
//	//act
//	shipment := &responses.Shipment{}
//	response := suite.Post("/shipments", payload, shipment)
//
//	//assert
//	suite.assert.Equal(http.StatusCreated, response.Code)
//	suite.assert.Equal(responses.NewShipmentFromModel(shipment1), shipment)
//}
