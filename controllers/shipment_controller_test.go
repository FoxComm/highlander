package controllers

import (
	"database/sql"
	"net/http"
	"testing"

	//"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"time"
)

type shipmentControllerTestSuite struct {
	GeneralControllerTestSuite
	shipmentService            *mocks.ShipmentServiceMock
	addressService             *mocks.AddressServiceMock
	shipmentLineItemService    *mocks.ShipmentLineItemServiceMock
	shipmentTransactionService *mocks.ShipmentTransactionServiceMock
}

func TestShipmentControllerSuite(t *testing.T) {
	suite.Run(t, new(shipmentControllerTestSuite))
}

func (suite *shipmentControllerTestSuite) SetupSuite() {
	suite.router = gin.New()

	suite.shipmentService = &mocks.ShipmentServiceMock{}
	suite.addressService = &mocks.AddressServiceMock{}
	suite.shipmentLineItemService = &mocks.ShipmentLineItemServiceMock{}
	suite.shipmentTransactionService = &mocks.ShipmentTransactionServiceMock{}

	controller := NewShipmentController(suite.shipmentService, suite.addressService, suite.shipmentLineItemService, suite.shipmentTransactionService)
	controller.SetUp(suite.router.Group("/shipments"))

	suite.assert = assert.New(suite.T())
}

func (suite *shipmentControllerTestSuite) TearDownTest() {
	// clear mock calls expectations after each test
	suite.shipmentService.ExpectedCalls = []*mock.Call{}
	suite.shipmentService.Calls = []mock.Call{}
	suite.addressService.ExpectedCalls = []*mock.Call{}
	suite.addressService.Calls = []mock.Call{}
	suite.shipmentLineItemService.ExpectedCalls = []*mock.Call{}
	suite.shipmentLineItemService.Calls = []mock.Call{}
	suite.shipmentTransactionService.ExpectedCalls = []*mock.Call{}
	suite.shipmentTransactionService.Calls = []mock.Call{}
}

func (suite *shipmentControllerTestSuite) Test_GetShipmentByID_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.shipmentService.On("GetShipmentByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := responses.Error{}
	response := suite.Get("/shipments/1", &errors)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(errors.Errors))
	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])

	//assert all expectations were met
	suite.shipmentService.AssertExpectations(suite.T())
}

func (suite *shipmentControllerTestSuite) Test_GetShipmentByID_Found_ReturnsRecord() {
	//arrange
	shipment1 := suite.getTestShipment1()
	suite.shipmentService.On("GetShipmentByID", uint(1)).Return(shipment1, nil).Once()
	address1 := suite.getTestAddess1()
	suite.addressService.On("GetAddressByID", uint(1)).Return(address1, nil).Once()
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	shipmentLineItem2 := suite.getTestShipmentLineItem2()
	suite.shipmentLineItemService.On("GetShipmentLineItemsByShipmentID", uint(1)).Return([]*models.ShipmentLineItem{
		shipmentLineItem1,
		shipmentLineItem2,
	}, nil).Once()
	shipmentTransaction1 := suite.getTestShipmentTransaction1(shipment1.ID)
	shipmentTransaction2 := suite.getTestShipmentTransaction2(shipment1.ID)
	shipmentTransaction3 := suite.getTestShipmentTransaction3(shipment1.ID)
	suite.shipmentTransactionService.On("GetShipmentTransactionsByShipmentID", uint(1)).Return([]*models.ShipmentTransaction{
		shipmentTransaction1,
		shipmentTransaction2,
		shipmentTransaction3,
	}, nil).Once()

	//act
	shipment := responses.Shipment{}
	response := suite.Get("/shipments/1", &shipment)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(shipment1.ID, shipment.ID)
	suite.assert.Equal(address1.ID, shipment.Address.ID)
	suite.assert.Equal(shipmentLineItem1.ID, shipment.LineItems[0].ID)
	suite.assert.Equal(shipmentLineItem2.ID, shipment.LineItems[1].ID)
	suite.assert.Equal(shipmentTransaction1.ID, shipment.Transactions.CreditCards[0].ID)
	suite.assert.Equal(shipmentTransaction2.ID, shipment.Transactions.GiftCards[0].ID)
	suite.assert.Equal(shipmentTransaction3.ID, shipment.Transactions.StoreCredits[0].ID)

	//assert all expectations were met
	suite.shipmentService.AssertExpectations(suite.T())
}

func (suite *shipmentControllerTestSuite) getTestShipment1() *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: uint(1)}, uint(1), "BR10007", "pending",
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *shipmentControllerTestSuite) getTestShipment2() *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: uint(2)}, uint(2), "BR10008", "delivered",
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *shipmentControllerTestSuite) getTestAddess1() *models.Address {
	return &models.Address{gormfox.Base{ID: uint(1)}, "Home address", uint(1), "Texas", "75231",
		"Some st, 335", sql.NullString{String: "", Valid: false}, "19527352893"}
}

func (suite *shipmentControllerTestSuite) getTestAddess2() *models.Address {
	return &models.Address{gormfox.Base{ID: uint(2)}, "Another address", uint(1), "Florida", "31223",
		"Other st, 235", sql.NullString{String: "", Valid: false}, "18729327642"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentLineItem1() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(1)}, uint(1), "AR001", "SKU-TEST1", "Some Name",
		uint(4999), "https://test.com/image1.png", "pending"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentLineItem2() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(2)}, uint(2), "AR002", "SKU-TEST2", "Some other Name",
		uint(5999), "https://test.com/image2.png", "shipped"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentLineItem3() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(3)}, uint(2), "AR003", "SKU-TEST3", "Other Name",
		uint(6999), "https://test.com/image3.png", "delivered"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentTransaction1(shipmentID uint) *models.ShipmentTransaction {
	return &models.ShipmentTransaction{uint(1), shipmentID, models.TransactionCreditCard,
		`{"brand":"Visa","holderName":"John Smith","lastFour":"1324","expMonth":10,"expYear":2017}`, time.Unix(1, 0), 4999}
}

func (suite *shipmentControllerTestSuite) getTestShipmentTransaction2(shipmentID uint) *models.ShipmentTransaction {
	return &models.ShipmentTransaction{uint(2), shipmentID, models.TransactionGiftCard,
		`{"code":"A1236BC38EE2265G"}`, time.Unix(2, 0), 5999}
}

func (suite *shipmentControllerTestSuite) getTestShipmentTransaction3(shipmentID uint) *models.ShipmentTransaction {
	return &models.ShipmentTransaction{uint(3), shipmentID, models.TransactionStoreCredit,
		`{}`, time.Unix(3, 0), 6999}
}