package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"fmt"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type InventoryServiceIntegrationTestSuite struct {
	GeneralServiceTestSuite
	itemResp       *models.StockItem
	service        InventoryService
	summaryService SummaryService
	sl             *models.StockLocation
	sku            string
}

func TestInventoryServiceIntegrationSuite(t *testing.T) {
	suite.Run(t, new(InventoryServiceIntegrationTestSuite))
}

func (suite *InventoryServiceIntegrationTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	tasks.TruncateTables(suite.db, []string{
		"stock_locations",
		"skus",
	})

	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)

	stockLocationService := NewStockLocationService(suite.db)

	suite.summaryService = NewSummaryService(suite.db)
	suite.service = &inventoryService{stockItemRepository, unitRepository, suite.summaryService, suite.db, nil}

	suite.sl, _ = stockLocationService.CreateLocation(fixtures.GetStockLocation())

	sku := fixtures.GetSKU()
	suite.Nil(suite.db.Create(sku).Error)

	suite.sku = sku.Code
}

func (suite *InventoryServiceIntegrationTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"stock_item_transactions",
		"inventory_search_view",
		"inventory_transactions_search_view",
	})
}

func (suite *InventoryServiceIntegrationTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *InventoryServiceIntegrationTestSuite) Test_CreateStockItem_SummaryCreation() {
	_, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)

	summary, err := suite.summaryService.GetSummaryBySKU(suite.sku)

	suite.Nil(err)
	suite.Equal(4, len(summary))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_IncrementStockItemUnits_SummaryUpdate() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 5)))

	summary, err := suite.summaryService.GetSummaryBySKU(suite.sku)

	suite.Nil(err)
	suite.Equal(5, summary[0].OnHand)
	suite.Equal(5, summary[0].AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_IncrementStockItemUnits_SummaryUpdate_WithTransaction() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)
	txn := suite.db.Begin()
	suite.Nil(suite.service.WithTransaction(txn).IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 5)))
	txn.Commit()

	summary, err := suite.summaryService.GetSummaryBySKU(suite.sku)

	suite.Nil(err)
	suite.Equal(5, summary[0].OnHand)
	suite.Equal(5, summary[0].AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_DecrementStockItemUnits_SummaryUpdate() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	err = suite.service.DecrementStockItemUnits(stockItem.ID, models.Sellable, 7)
	suite.Nil(err)

	summary, err := suite.summaryService.GetSummaryBySKU(suite.sku)

	suite.Nil(err)
	suite.Equal(3, summary[0].OnHand)
	suite.Equal(3, summary[0].AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_MultipleSKUsSummary() {
	sku1 := suite.createSKU("TEST-RESERVATION-A")
	sku2 := suite.createSKU("TEST-RESERVATION-B")

	stockItem1, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku1.Code))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem1.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem1, 5)))

	stockItem2, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, sku2.Code))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem2.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem2, 5)))

	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: sku1.Code, Qty: 3},
			payloads.ItemReservation{SKU: sku2.Code, Qty: 5},
		},
	}

	suite.Nil(suite.service.HoldItems(payload))

	summary1, err := suite.summaryService.GetSummaryBySKU(sku1.Code)
	suite.Nil(err)

	summary2, err := suite.summaryService.GetSummaryBySKU(sku2.Code)
	suite.Nil(err)

	suite.Equal(int(payload.Items[0].Qty), summary1[0].OnHold)
	suite.Equal(int(payload.Items[1].Qty), summary2[0].OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_SubsequentSummary() {
	suite.createSKU(suite.sku)
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: suite.sku, Qty: 3},
		},
	}

	suite.Nil(suite.service.HoldItems(payload))

	summary, err := suite.summaryService.GetSummaryBySKU(suite.sku)
	suite.Nil(err)

	suite.Equal(int(payload.Items[0].Qty), summary[0].OnHold)

	payload = &payloads.Reservation{
		RefNum: "BR10002",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: suite.sku, Qty: 5},
		},
	}

	suite.Nil(suite.service.HoldItems(payload))

	summary, err = suite.summaryService.GetSummaryBySKU(suite.sku)
	suite.Nil(err)

	suite.Equal(8, summary[0].OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_Summary() {
	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: suite.sku, Qty: 1},
		},
	}

	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 1)))
	suite.Nil(suite.service.HoldItems(payload))

	suite.Nil(suite.service.ReleaseItems(payload.RefNum))

	summary, err := suite.summaryService.GetSummaryBySKU(suite.sku)
	suite.Nil(err)

	suite.Equal(0, summary[0].OnHold, "No stock item units should be onHold")
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ShipItems_Summary() {
	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: suite.sku, Qty: 1},
		},
	}

	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)
	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 1)))
	suite.Nil(suite.service.HoldItems(payload))
	suite.Nil(suite.service.ReserveItems(payload.RefNum))
	suite.Nil(suite.service.ShipItems(payload.RefNum))

	summary, err := suite.summaryService.GetSummaryBySKU(suite.sku)
	suite.Nil(err)

	suite.Equal(0, summary[0].OnHand, "No stock item units should be onHand")
	suite.Equal(0, summary[0].OnHold, "No stock item units should be onHold")
	suite.Equal(0, summary[0].Reserved, "No stock item units should be Reserved")
	suite.Equal(1, summary[0].Shipped, "1 stock item unit should be Shipped")
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, err := suite.service.GetAFSByID(stockItem.ID, models.Sellable)

	suite.Nil(err)
	suite.Equal(stockItem.ID, afs.StockItemID)
	suite.Equal(stockItem.SKU, afs.SKU)
	suite.Equal(10, afs.AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID_NotFound() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, err := suite.service.GetAFSByID(uint(222), models.Sellable)

	suite.Equal(fmt.Errorf(repositories.ErrorStockItemNotFound, 222), err)
	suite.Nil(afs)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, _ := suite.service.GetAFSBySKU(stockItem.SKU, models.Sellable)

	suite.Equal(stockItem.SKU, afs.SKU)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU_NotFound() {
	stockItem, err := suite.service.CreateStockItem(fixtures.GetStockItem(suite.sl.ID, suite.sku))
	suite.Nil(err)

	suite.Nil(suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, fixtures.GetStockItemUnits(stockItem, 10)))

	afs, err := suite.service.GetAFSBySKU("BLA-BLA-SKU", models.Sellable)

	suite.Equal(gorm.ErrRecordNotFound, err)
	suite.Nil(afs)
}

func (suite *InventoryServiceIntegrationTestSuite) createSKU(code string) *models.SKU {
	sku := fixtures.GetSKU()
	sku.Code = code
	sku.RequiresInventoryTracking = true
	suite.Nil(suite.db.Create(sku).Error)
	return sku
}
