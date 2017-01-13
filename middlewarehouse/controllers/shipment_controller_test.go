package controllers

import (
	"database/sql"
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type shipmentControllerTestSuite struct {
	GeneralControllerTestSuite
	db               *gorm.DB
	inventoryService services.InventoryService
	shipmentService  services.ShipmentService
	summaryService   services.SummaryService
	shippingMethod   *models.ShippingMethod
	stockItem        *models.StockItem
	stockLocation    *models.StockLocation
}

func TestShipmentControllerSuite(t *testing.T) {
	suite.Run(t, new(shipmentControllerTestSuite))
}

func (suite *shipmentControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()

	logger := &dummyLogger{}
	suite.summaryService = services.NewSummaryService(suite.db)
	suite.inventoryService = services.NewInventoryService(suite.db)
	suite.shipmentService = services.NewShipmentService(
		suite.db,
		suite.inventoryService,
		suite.summaryService,
		logger,
	)

	controller := NewShipmentController(suite.shipmentService)
	controller.SetUp(suite.router.Group("/shipments"))

	tasks.TruncateTables(suite.db, []string{
		"carriers",
		"shipping_methods",
		"inventory_search_view",
		"stock_locations",
	})

	carrier := &models.Carrier{Name: "FedEx", TrackingTemplate: "test.png"}
	suite.Nil(suite.db.Create(carrier).Error)

	suite.shippingMethod = &models.ShippingMethod{
		CarrierID:    carrier.ID,
		Name:         "Test",
		Code:         "EXPRESS",
		ShippingType: models.ShippingTypeFlat,
		Cost:         0,
	}
	suite.Nil(suite.db.Create(suite.shippingMethod).Error)

	suite.stockLocation = fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(suite.stockLocation).Error)
}

func (suite *shipmentControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"inventory_search_view",
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"shipments",
	})

	stockItem := models.StockItem{
		SKU:             "SKU-TEST1",
		StockLocationID: suite.stockLocation.ID,
		DefaultUnitCost: 0,
	}

	var err error
	suite.stockItem, err = suite.inventoryService.CreateStockItem(&stockItem)
	suite.Nil(err)

	units := []*models.StockItemUnit{}
	for i := 0; i < 2; i++ {
		unit := models.StockItemUnit{
			StockItemID: suite.stockItem.ID,
			UnitCost:    0,
			Status:      models.StatusOnHold,
			Type:        models.Sellable,
			RefNum:      sql.NullString{String: "BR10005", Valid: true},
		}
		units = append(units, &unit)
	}

	suite.Nil(suite.inventoryService.IncrementStockItemUnits(suite.stockItem.ID, models.Sellable, units))
}

func (suite *shipmentControllerTestSuite) Test_GetShipmentsByOrder_NotFound_ReturnsNoError() {
	//act
	shipments := responses.Shipments{}
	response := suite.Get("/shipments/BR1005", &shipments)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(0, len(shipments.Shipments))
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

	//act
	shipment := &responses.Shipment{}
	response := suite.Post("/shipments", payload, shipment)

	//assert
	suite.Equal(http.StatusCreated, response.Code)
	suite.Equal("BR10005", shipment.OrderRefNum)

	var units []*models.StockItemUnit
	err := suite.db.
		Where("stock_item_id = ?", suite.stockItem.ID).
		Where("type = ?", models.Sellable).
		Where("status = ?", models.StatusReserved).
		Where("ref_num = ?", shipment.OrderRefNum).
		Find(&units).
		Error
	suite.Nil(err)
	suite.Equal(2, len(units))
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
	shipment1, err := suite.shipmentService.CreateShipment(fixtures.GetShipmentShort(uint(1)))
	suite.Nil(err)
	payload := payloads.UpdateShipment{State: "shipped"}

	//act
	shipment := &responses.Shipment{}
	url := fmt.Sprintf("/shipments/for-order/%s", shipment1.OrderRefNum)
	response := suite.Patch(url, payload, shipment)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	//suite.Equal(responses.NewShipmentFromModel(shipment1), shipment)
}

type dummyLogger struct{}

func (d dummyLogger) Log(activity activities.ISiteActivity) error { return nil }
