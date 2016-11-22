package controllers

import (
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/controllers/mocks"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type shipmentControllerTestSuite struct {
	GeneralControllerTestSuite
	shipmentService *mocks.ShipmentServiceMock
}

func TestShipmentControllerSuite(t *testing.T) {
	suite.Run(t, new(shipmentControllerTestSuite))
}

func (suite *shipmentControllerTestSuite) SetupSuite() {
	suite.router = gin.New()

	suite.shipmentService = &mocks.ShipmentServiceMock{}

	controller := NewShipmentController(suite.shipmentService)
	controller.SetUp(suite.router.Group("/shipments"))
}

func (suite *shipmentControllerTestSuite) TearDownTest() {
	// clear mock calls expectations after each test
	suite.shipmentService.AssertExpectations(suite.T())
	suite.shipmentService.ExpectedCalls = []*mock.Call{}
	suite.shipmentService.Calls = []mock.Call{}
}

func (suite *shipmentControllerTestSuite) Test_GetShipmentsByOrder_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.shipmentService.On("GetShipmentsByOrder", "BR1005").Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := responses.Error{}
	response := suite.Get("/shipments/BR1005", &errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
}

// TODO: Re-enable later
//func (suite *shipmentControllerTestSuite) Test_GetShipmentsByOrder_Found_ReturnsRecords() {
////arrange
//shipment1 := fixtures.GetShipmentShort(uint(1))
//shipment2 := fixtures.GetShipmentShort(uint(2))
//suite.shipmentService.On("GetShipmentsByOrder", shipment1.ReferenceNumber).Return([]*models.Shipment{
//shipment1,
//}, nil).Once()
//suite.shipmentService.On("GetShipmentsByOrder", shipment2.ReferenceNumber).Return([]*models.Shipment{
//shipment2,
//}, nil).Once()

////act
//shipments := []*responses.Shipment{}
//response := suite.Get(fmt.Sprintf("/shipments/%s,%s", shipment1.ReferenceNumber, shipment2.ReferenceNumber), &shipments)

////assert
//suite.Equal(http.StatusOK, response.Code)
//suite.Equal(2, len(shipments))
//suite.Equal(responses.NewShipmentFromModel(shipment1), shipments[0])
//suite.Equal(responses.NewShipmentFromModel(shipment2), shipments[1])
//}

func (suite *shipmentControllerTestSuite) Test_CreateShipment_ReturnsRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	payload := fixtures.ToShipmentPayload(shipment1)
	suite.shipmentService.On("CreateShipment", models.NewShipmentFromPayload(payload)).Return(shipment1, nil).Once()

	//act
	shipment := &responses.Shipment{}
	response := suite.Post("/shipments", payload, shipment)

	//assert
	suite.Equal(http.StatusCreated, response.Code)
	expectedResp, err := responses.NewShipmentFromModel(shipment1)
	suite.Nil(err)
	suite.Equal(expectedResp, shipment)
}

// TODO: Re-enable
//func (suite *shipmentControllerTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
////arrange
//shipment1 := fixtures.GetShipment(uint(1), uint(1), &models.ShippingMethod{},
//uint(1), &models.Address{}, []models.ShipmentLineItem{})
//suite.shipmentService.
//On("UpdateShipment", shipment1).
//Return(nil, gorm.ErrRecordNotFound).Once()

////act
//errors := &responses.Error{}
//response := suite.Put("/shipments/1", fixtures.ToShipmentPayload(shipment1), errors)

////assert
//suite.Equal(http.StatusNotFound, response.Code)
//suite.Equal(1, len(errors.Errors))
//suite.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
//}

func (suite *shipmentControllerTestSuite) Test_UpdateShipment_Found_ReturnsRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	shipmentLineItem1 := *fixtures.GetShipmentLineItem(uint(1), 0, 0)
	shipmentLineItem2 := *fixtures.GetShipmentLineItem(uint(2), 0, 0)

	updateShipment := fixtures.GetShipment(
		uint(0), "", shipment1.ShippingMethodCode, &models.ShippingMethod{},
		shipment1.AddressID, &shipment1.Address, []models.ShipmentLineItem{shipmentLineItem1, shipmentLineItem2})
	updateShipment.ReferenceNumber = ""
	updateShipment.OrderRefNum = "BR10001"

	suite.shipmentService.
		On("UpdateShipmentForOrder", updateShipment).
		Return(shipment1, nil).Once()

	//act
	shipment := &responses.Shipment{}
	response := suite.Patch("/shipments/for-order/BR10001", fixtures.ToShipmentPayload(shipment1), shipment)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	//suite.Equal(responses.NewShipmentFromModel(shipment1), shipment)
}
