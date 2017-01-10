package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"github.com/stretchr/testify/suite"
)

type ShipmentServiceTestSuite struct {
	GeneralServiceTestSuite
	service          IShipmentService
	inventoryService InventoryService
	summaryService   SummaryService
}

func TestShipmentServiceSuite(t *testing.T) {
	suite.Run(t, new(ShipmentServiceTestSuite))
}

func (suite *ShipmentServiceTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)
	shipmentRepository := repositories.NewShipmentRepository(suite.db)

	suite.summaryService = NewSummaryService(suite.db)
	suite.inventoryService = &inventoryService{stockItemRepository, unitRepository, suite.summaryService, nil}
	logger := &dummyLogger{}

	suite.service = NewShipmentService(
		suite.db,
		suite.inventoryService,
		suite.summaryService,
		shipmentRepository,
		unitRepository,
		logger,
	)
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
		"inventory_search_view",
		"inventory_transactions_search_view",
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
	stockItem, err := suite.inventoryService.CreateStockItem(stockItem)
	suite.Nil(err)

	suite.Nil(suite.inventoryService.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 5)))
	suite.Nil(suite.inventoryService.HoldItems(shipment1.OrderRefNum, map[string]int{stockItem.SKU: 2}))

	//act
	shipment, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1.ShippingMethodCode, shipment.ShippingMethodCode)
	suite.Equal(shipment1.OrderRefNum, shipment.OrderRefNum)
	suite.Equal(shipment1.State, shipment.State)

	// check summary updated properly
	summary, err := suite.summaryService.GetSummaryBySKU(stockItem.SKU)

	suite.Nil(err)
	suite.Equal(5, summary[0].OnHand)
	suite.Equal(0, summary[0].OnHold)
	suite.Equal(2, summary[0].Reserved)
	suite.Equal(3, summary[0].AFS)
}

func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_Partial_ReturnsUpdatedRecord() {
	//arrange
	shipment := fixtures.GetShipmentShort(uint(1))

	suite.Nil(suite.db.Set("gorm:save_associations", false).Create(&shipment.Address).Error)
	suite.Nil(suite.db.Create(&shipment.ShippingMethod.Carrier).Error)
	suite.Nil(suite.db.Create(&shipment.ShippingMethod).Error)
	shipment.AddressID = shipment.Address.ID

	stockLocation := fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(stockLocation).Error)

	stockItem := fixtures.GetStockItem(stockLocation.ID, shipment.ShipmentLineItems[0].SKU)
	stockItem, err := suite.inventoryService.CreateStockItem(stockItem)
	suite.Nil(err)

	suite.Nil(suite.inventoryService.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 5)))
	suite.Nil(suite.inventoryService.HoldItems(shipment.OrderRefNum, map[string]int{stockItem.SKU: 2}))

	_, err = suite.service.CreateShipment(shipment)
	suite.Nil(err)

	//act
	payload := payloads.UpdateShipment{State: "shipped"}
	updateShipment := payload.Model()
	updateShipment.ID = shipment.ID

	//act
	updated, err := suite.service.UpdateShipment(updateShipment)

	//assert
	suite.Nil(err)
	suite.Equal(shipment.ID, updated.ID)
	suite.Equal(shipment.OrderRefNum, updated.OrderRefNum)
	suite.Equal(models.ShipmentStateShipped, updated.State)

	// check summary updated properly
	summary, err := suite.summaryService.GetSummaryBySKU(stockItem.SKU)

	suite.Nil(err)
	suite.Equal(3, summary[0].OnHand)
	suite.Equal(0, summary[0].OnHold)
	suite.Equal(0, summary[0].Reserved)
	suite.Equal(3, summary[0].AFS)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_Failed() {
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
	stockItem, err := suite.inventoryService.CreateStockItem(stockItem)
	suite.Nil(err)

	suite.Nil(suite.inventoryService.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 5)))
	suite.Nil(suite.inventoryService.HoldItems(shipment1.OrderRefNum, map[string]int{stockItem.SKU: 1}))

	// check summary updated properly before shipment created
	summary, err := suite.summaryService.GetSummaryBySKU(stockItem.SKU)

	suite.Nil(err)
	suite.Equal(5, summary[0].OnHand)
	suite.Equal(1, summary[0].OnHold)
	suite.Equal(0, summary[0].Reserved)
	suite.Equal(4, summary[0].AFS)

	//act
	_, err = suite.service.CreateShipment(shipment1)

	//assert
	suite.NotNil(err)

	// check summary was not updated properly
	summary, err = suite.summaryService.GetSummaryBySKU(stockItem.SKU)

	suite.Nil(err)
	suite.Equal(5, summary[0].OnHand)
	suite.Equal(1, summary[0].OnHold)
	suite.Equal(0, summary[0].Reserved)
	suite.Equal(4, summary[0].AFS)
}

type dummyLogger struct{}

func (d dummyLogger) Log(activity activities.ISiteActivity) error { return nil }
