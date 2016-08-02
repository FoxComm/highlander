package controllers

import (
	"database/sql"
	"net/http"
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
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
	suite.shipmentService.AssertExpectations(suite.T())
	suite.shipmentService.ExpectedCalls = []*mock.Call{}
	suite.shipmentService.Calls = []mock.Call{}
	suite.addressService.AssertExpectations(suite.T())
	suite.addressService.ExpectedCalls = []*mock.Call{}
	suite.addressService.Calls = []mock.Call{}
	suite.shipmentLineItemService.AssertExpectations(suite.T())
	suite.shipmentLineItemService.ExpectedCalls = []*mock.Call{}
	suite.shipmentLineItemService.Calls = []mock.Call{}
	suite.shipmentTransactionService.AssertExpectations(suite.T())
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
}

func (suite *shipmentControllerTestSuite) Test_GetShipmentByID_Found_ReturnsRecord() {
	//arrange
	shipment1 := suite.getTestShipment1(uint(1))
	address1 := suite.getTestAddess1(uint(1))
	shipmentLineItem1 := suite.getTestShipmentLineItem1(uint(1), shipment1.ID)
	shipmentLineItem2 := suite.getTestShipmentLineItem2(uint(2), shipment1.ID)
	shipmentTransaction1 := suite.getTestShipmentTransaction1(uint(1), shipment1.ID)
	shipmentTransaction2 := suite.getTestShipmentTransaction2(uint(1), shipment1.ID)
	shipmentTransaction3 := suite.getTestShipmentTransaction3(uint(1), shipment1.ID)
	suite.shipmentService.On("GetShipmentByID", uint(1)).Return(shipment1, nil).Once()
	suite.addressService.On("GetAddressByID", uint(1)).Return(address1, nil).Once()
	suite.shipmentLineItemService.On("GetShipmentLineItemsByShipmentID", uint(1)).Return([]*models.ShipmentLineItem{
		shipmentLineItem1,
		shipmentLineItem2,
	}, nil).Once()
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
}

func (suite *shipmentControllerTestSuite) Test_CreateShipment_ReturnsRecord() {
	//arrange
	shipment1 := suite.getTestShipment1(uint(1))
	payload := &payloads.Shipment{shipment1.ShippingMethodID, shipment1.ReferenceNumber, shipment1.State,
		nil, shipment1.ShipmentDate.String, shipment1.EstimatedArrival.String, shipment1.DeliveredDate.String, payloads.Address{}}
	address1 := suite.getTestAddess1(uint(1))
	payload.Address = payloads.Address{address1.Name, address1.RegionID, address1.City,
		address1.Zip, address1.Address1, &address1.Address2.String, address1.PhoneNumber}
	shipmentLineItem1 := suite.getTestShipmentLineItem1(uint(1), shipment1.ID)
	shipmentLineItem2 := suite.getTestShipmentLineItem2(uint(2), shipment1.ID)
	payload.LineItems = []payloads.ShipmentLineItem{
		{shipmentLineItem1.ReferenceNumber, shipmentLineItem1.SKU, shipmentLineItem1.Name,
			shipmentLineItem1.Price, shipmentLineItem1.ImagePath, shipmentLineItem1.State},
		{shipmentLineItem2.ReferenceNumber, shipmentLineItem2.SKU, shipmentLineItem2.Name,
			shipmentLineItem2.Price, shipmentLineItem2.ImagePath, shipmentLineItem2.State},
	}
	suite.shipmentService.On("CreateShipment",
		models.NewShipmentFromPayload(payload),
		models.NewAddressFromPayload(&payload.Address),
		[]*models.ShipmentLineItem{
			models.NewShipmentLineItemFromPayload(&payload.LineItems[0]),
			models.NewShipmentLineItemFromPayload(&payload.LineItems[1]),
		}).
		Return(shipment1, nil).Once()
	suite.addressService.On("GetAddressByID", uint(1)).Return(address1, nil).Once()
	suite.shipmentLineItemService.On("GetShipmentLineItemsByShipmentID", uint(1)).Return([]*models.ShipmentLineItem{
		shipmentLineItem1,
		shipmentLineItem2,
	}, nil).Once()
	suite.shipmentTransactionService.On("GetShipmentTransactionsByShipmentID", uint(1)).Return([]*models.ShipmentTransaction{}, nil).Once()

	//act
	shipment := responses.Shipment{}
	response := suite.Post("/shipments/", payload, &shipment)

	//assert
	suite.assert.Equal(http.StatusCreated, response.Code)
	suite.assert.Equal(shipment1.ID, shipment.ID)
	suite.assert.Equal(address1.ID, shipment.Address.ID)
	suite.assert.Equal(shipmentLineItem1.ID, shipment.LineItems[0].ID)
	suite.assert.Equal(shipmentLineItem2.ID, shipment.LineItems[1].ID)
}

func (suite *shipmentControllerTestSuite) getTestShipment1(id uint) *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: id}, uint(1), "BR10007", "pending",
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *shipmentControllerTestSuite) getTestShipment2(id uint) *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: id}, uint(2), "BR10008", "delivered",
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *shipmentControllerTestSuite) getTestAddess1(id uint) *models.Address {
	return &models.Address{gormfox.Base{ID: id}, "Home address", uint(1), "Texas", "75231",
		"Some st, 335", sql.NullString{String: "", Valid: false}, "19527352893"}
}

func (suite *shipmentControllerTestSuite) getTestAddess2(id uint) *models.Address {
	return &models.Address{gormfox.Base{ID: id}, "Another address", uint(1), "Florida", "31223",
		"Other st, 235", sql.NullString{String: "", Valid: false}, "18729327642"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentLineItem1(id uint, shipmentID uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: id}, shipmentID, "AR001", "SKU-TEST1", "Some Name",
		uint(4999), "https://test.com/image1.png", "pending"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentLineItem2(id uint, shipmentID uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: id}, shipmentID, "AR002", "SKU-TEST2", "Some other Name",
		uint(5999), "https://test.com/image2.png", "shipped"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentLineItem3(id uint, shipmentID uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: id}, shipmentID, "AR003", "SKU-TEST3", "Other Name",
		uint(6999), "https://test.com/image3.png", "delivered"}
}

func (suite *shipmentControllerTestSuite) getTestShipmentTransaction1(id uint, shipmentID uint) *models.ShipmentTransaction {
	return &models.ShipmentTransaction{id, shipmentID, models.TransactionCreditCard,
		`{"brand":"Visa","holderName":"John Smith","lastFour":"1324","expMonth":10,"expYear":2017}`, time.Unix(1, 0), 4999}
}

func (suite *shipmentControllerTestSuite) getTestShipmentTransaction2(id uint, shipmentID uint) *models.ShipmentTransaction {
	return &models.ShipmentTransaction{id, shipmentID, models.TransactionGiftCard,
		`{"code":"A1236BC38EE2265G"}`, time.Unix(2, 0), 5999}
}

func (suite *shipmentControllerTestSuite) getTestShipmentTransaction3(id uint, shipmentID uint) *models.ShipmentTransaction {
	return &models.ShipmentTransaction{id, shipmentID, models.TransactionStoreCredit,
		`{}`, time.Unix(3, 0), 6999}
}
