package services

import (
	"math/rand"
	"testing"
	"time"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type InventoryServiceIntegrationTestSuite struct {
	suite.Suite
	itemResp *models.StockItem
	service  IInventoryService
	db       *gorm.DB
}

func TestInventoryServiceIntegrationSuite(t *testing.T) {
	suite.Run(t, new(InventoryServiceIntegrationTestSuite))
}

// Just a few helper functions!
func (suite *InventoryServiceIntegrationTestSuite) createStockLocation() (*models.StockLocation, error) {
	stockLocationService := NewStockLocationService(repositories.NewStockLocationRepository(suite.db))

	return stockLocationService.CreateLocation(&models.StockLocation{Type: "Warehouse", Name: "TEST-LOCATION"})
}

func (suite *InventoryServiceIntegrationTestSuite) createStockItem(sku string, qty int) (*models.StockItem, error) {
	stockItem := &models.StockItem{StockLocationID: 1, SKU: sku, DefaultUnitCost: 45000}
	resp, err := suite.service.CreateStockItem(stockItem)
	if err != nil {
		return nil, err
	}

	if qty > 0 {
		units := []*models.StockItemUnit{}
		for i := 0; i < qty; i++ {
			item := &models.StockItemUnit{
				StockItemID: stockItem.ID,
				UnitCost:    500,
				Type:        models.Sellable,
				Status:      models.StatusOnHand,
			}
			units = append(units, item)
		}

		err := suite.service.IncrementStockItemUnits(stockItem.ID, models.Sellable, units)
		if err != nil {
			return nil, err
		}
	}

	return resp, nil
}

func (suite *InventoryServiceIntegrationTestSuite) createReservation(skus []string, qty int, refNum string) error {
	r := rand.New(rand.NewSource(99))

	for _, sku := range skus {
		_, err := suite.createStockItem(sku, qty+r.Intn(10))

		if err != nil {
			return err
		}

		err = suite.service.ReserveItems(refNum, map[string]int{sku: qty})

		if err != nil {
			return err
		}
	}

	return nil
}

func (suite *InventoryServiceIntegrationTestSuite) SetupSuite() {
	suite.db, _ = config.DefaultConnection()

	summaryRepository := repositories.NewSummaryRepository(suite.db)
	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)

	summaryService := NewSummaryService(summaryRepository, stockItemRepository)
	suite.service = NewInventoryService(stockItemRepository, unitRepository, summaryService)
}

func (suite *InventoryServiceIntegrationTestSuite) SetupTest() {

	time.Sleep(100 * time.Millisecond)

	tasks.TruncateTables([]string{
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"stock_item_transactions",
		"stock_locations",
		"inventory_search_view",
	})

	suite.createStockLocation()
}

func (suite *InventoryServiceIntegrationTestSuite) Test_CreateStockItem() {
	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-CREATION"}
	resp, err := suite.service.CreateStockItem(stockItem)
	suite.Nil(err)
	suite.Equal(stockItem.SKU, resp.SKU)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_CreateStockItem_SummaryCreation() {

	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-SUMMARY-CREATED"}
	resp, err := suite.service.CreateStockItem(stockItem)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary := []*models.StockItemSummary{}
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&summary).Error
	suite.Nil(err)
	suite.Equal(4, len(summary))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetStockItemById() {
	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-FIND"}
	resp, err := suite.service.CreateStockItem(stockItem)
	suite.Nil(err)
	item, err := suite.service.GetStockItemById(resp.ID)
	suite.Nil(err)
	suite.Equal("TEST-FIND", item.SKU)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_CreateStockItem_EmptySKU() {
	stockItem := &models.StockItem{StockLocationID: 1}
	_, err := suite.service.CreateStockItem(stockItem)

	suite.NotNil(err)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_CreateExistingStockItem() {
	stockItem := &models.StockItem{
		StockLocationID: 1,
		SKU:             "TEST-UPSERT",
		DefaultUnitCost: 999,
	}

	resp, err := suite.service.CreateStockItem(stockItem)
	suite.Nil(err)

	var item models.StockItem
	err = suite.db.First(&item, resp.ID).Error
	suite.Nil(err)
	suite.Equal(stockItem.DefaultUnitCost, item.DefaultUnitCost)

	stockItemUpdate := &models.StockItem{
		StockLocationID: 1,
		SKU:             "TEST-UPSERT",
		DefaultUnitCost: 599,
	}

	respUpdate, err := suite.service.CreateStockItem(stockItemUpdate)
	suite.Nil(err)
	suite.Equal(resp.ID, respUpdate.ID)
	suite.Equal(resp.SKU, respUpdate.SKU)

	var itemUpdate models.StockItem
	err = suite.db.First(&itemUpdate, respUpdate.ID).Error
	suite.Nil(err)
	suite.Equal(stockItemUpdate.DefaultUnitCost, itemUpdate.DefaultUnitCost)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_IncrementStockItemUnits() {
	resp, err := suite.createStockItem("TEST-INCREMENT", 1)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	suite.Nil(err)
	suite.Equal(1, len(units))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_IncrementStockItemUnits_MultipleItems() {
	resp, err := suite.createStockItem("TEST-INCREMENT", 10)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	suite.Nil(err)
	suite.Equal(10, len(units))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_DecrementStockItemUnits() {
	resp, err := suite.createStockItem("TEST-DECREMENT", 10)

	err = suite.service.DecrementStockItemUnits(resp.ID, models.Sellable, 7)
	suite.Nil(err)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	suite.Nil(err)
	suite.Equal(3, len(units))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReserveItems_SingleSKU() {
	resp, err := suite.createStockItem("TEST-RESERVATION", 1)

	refNum := "BR10001"
	skus := map[string]int{"TEST-RESERVATION": 1}

	err = suite.service.ReserveItems(refNum, skus)
	suite.Nil(err)

	var units []models.StockItemUnit
	err = suite.db.Where("ref_num = ?", refNum).Find(&units).Error
	suite.Nil(err)
	suite.Equal(1, len(units))

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	// check if StockItemSummary.Reserved got updated for updated StockItem
	var summary models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	suite.Equal(len(skus), summary.OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReserveItems_MultipleSKUs() {
	sku1 := "TEST-RESERVATION-A"
	sku2 := "TEST-RESERVATION-B"

	resp1, err := suite.createStockItem(sku1, 5)
	resp2, err := suite.createStockItem(sku2, 5)

	refNum := "BR10001"
	skus := map[string]int{
		sku1: 5,
		sku2: 5,
	}

	err = suite.service.ReserveItems(refNum, skus)
	suite.Nil(err)

	var units []models.StockItemUnit
	err = suite.db.
		Where("ref_num = ?", refNum).
		Where("stock_item_id = ?", resp1.ID).
		Find(&units).Error
	suite.Nil(err)
	suite.Equal(5, len(units))

	err = suite.db.
		Where("ref_num = ?", refNum).
		Where("stock_item_id = ?", resp2.ID).
		Find(&units).Error
	suite.Nil(err)
	suite.Equal(skus[sku1], len(units))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReserveItems_StockItemChanged() {
	sku := "TEST-RESERVATION"
	suite.createStockItem(sku, 10)

	refNum := "BR10001"
	skus := map[string]int{sku: 1}

	suite.service.ReserveItems(refNum, skus)

	var units []models.StockItemUnit
	err := suite.db.Where("ref_num = ?", refNum).Find(&units).Error
	suite.Nil(err)
	suite.Equal(int(skus[sku]), len(units))
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReserveItems_NoOnHand() {
	refNum := "BR10001"
	skus := map[string]int{"TEST-DEFAULT": 1}

	err := suite.service.ReserveItems(refNum, skus)
	suite.NotNil(err)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReserveItems_NoSKU() {
	refNum := "BR10001"
	skus := map[string]int{"NO-SKU": 1}

	err := suite.service.ReserveItems(refNum, skus)
	suite.NotNil(err)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_MultipleSKUsSummary() {
	sku1 := "TEST-RESERVATION-A"
	sku2 := "TEST-RESERVATION-B"

	resp1, _ := suite.createStockItem(sku1, 5)
	resp2, _ := suite.createStockItem(sku2, 5)

	refNum := "BR10001"
	skus := map[string]int{
		sku1: 3,
		sku2: 5,
	}

	suite.service.ReserveItems(refNum, skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	var summary1 models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp1.ID).First(&summary1)
	suite.Equal(skus[sku1], summary1.OnHold)

	var summary2 models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp2.ID).First(&summary2)
	suite.Equal(skus[sku2], summary2.OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_SubsequentSummary() {
	sku := "TEST-RESERVATION-A"
	resp, _ := suite.createStockItem(sku, 10)

	skus := map[string]int{sku: 3}

	suite.service.ReserveItems("BR10001", skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	var summary models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	suite.Equal(skus[sku], summary.OnHold)

	skus[sku] = 5

	suite.service.ReserveItems("BR10002", skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	suite.Equal(8, summary.OnHold)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_NoReservedSKUs() {
	suite.createStockItem("TEST-RESERVATION-A", 1)

	err := suite.service.ReleaseItems("BR10001")
	suite.NotNil(err, "Should not be able to unreserve items while there are no reservations")
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_Single() {
	skus := []string{"TEST-UNRESERVATION-A"}
	refNum := "BR10001"
	err := suite.createReservation(skus, 1, refNum)
	suite.Nil(err)

	onHoldUnitsCount := 0
	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", refNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.Equal(1, onHoldUnitsCount, "There should be one unit in onHold status")

	// send release request and check if it was processed successfully
	err = suite.service.ReleaseItems(refNum)
	suite.Nil(err, "Reservation should be successfully removed")

	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", refNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.Equal(0, onHoldUnitsCount, "There should not be units in onHold status")
}

func (suite *InventoryServiceIntegrationTestSuite) Test_ReleaseItems_Summary() {
	skus := []string{"TEST-UNRESERVATION-A"}
	refNum := "BR10001"
	reservedCount := 1
	err := suite.createReservation(skus, reservedCount, refNum)
	suite.Nil(err)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	var summary models.StockItemSummary
	suite.db.Where("type = ?", models.Sellable).First(&summary)

	suite.Equal(reservedCount, summary.OnHold, "One stock item unit should be onHold")

	suite.service.ReleaseItems(refNum)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	suite.db.Where("type = ?", models.Sellable).First(&summary)
	suite.Equal(0, summary.OnHold, "No stock item units should be onHold")
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID() {
	resp, _ := suite.createStockItem("TEST-DECREMENT", 10)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	afs, err := suite.service.GetAFSByID(resp.ID, models.Sellable)

	suite.Nil(err)
	suite.Equal(resp.ID, afs.StockItemID)
	suite.Equal(resp.SKU, afs.SKU)
	suite.Equal(10, afs.AFS)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSByID_NotFound() {
	suite.createStockItem("TEST-DECREMENT", 10)

	afs, err := suite.service.GetAFSByID(uint(222), models.Sellable)

	suite.Equal(gorm.ErrRecordNotFound, err)
	suite.Nil(afs)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU() {
	resp, _ := suite.createStockItem("TEST-DECREMENT", 10)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	afs, _ := suite.service.GetAFSBySKU(resp.SKU, models.Sellable)

	suite.Equal(resp.SKU, afs.SKU)
}

func (suite *InventoryServiceIntegrationTestSuite) Test_GetAFSBySKU_NotFound() {
	suite.createStockItem("TEST-DECREMENT", 10)

	afs, err := suite.service.GetAFSBySKU("BLA-BLA-SKU", models.Sellable)

	suite.Equal(gorm.ErrRecordNotFound, err)
	suite.Nil(afs)
}
