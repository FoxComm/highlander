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
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type InventoryServiceTestSuite struct {
	suite.Suite
	itemResp *models.StockItem
	service  IInventoryService
	db       *gorm.DB
	assert   *assert.Assertions
}

func TestInventoryServiceSuite(t *testing.T) {
	suite.Run(t, new(InventoryServiceTestSuite))
}

// Just a few helper functions!
func (suite *InventoryServiceTestSuite) createStockLocation() (*models.StockLocation, error) {
	stockLocationService := NewStockLocationService(repositories.NewStockLocationRepository(suite.db))

	return stockLocationService.CreateLocation(&models.StockLocation{Type: "Warehouse", Name: "TEST-LOCATION"})
}

func (suite *InventoryServiceTestSuite) createStockItem(sku string, qty int) (*models.StockItem, error) {
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

func (suite *InventoryServiceTestSuite) createReservation(skus []string, qty int, refNum string) error {
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

func (suite *InventoryServiceTestSuite) SetupSuite() {
	suite.db, _ = config.DefaultConnection()
	suite.assert = assert.New(suite.T())

	summaryRepository := repositories.NewSummaryRepository(suite.db)
	stockItemRepository := repositories.NewStockItemRepository(suite.db)
	unitRepository := repositories.NewStockItemUnitRepository(suite.db)

	summaryService := NewSummaryService(summaryRepository, stockItemRepository)
	suite.service = NewInventoryService(stockItemRepository, unitRepository, summaryService)
}

func (suite *InventoryServiceTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
		"reservations",
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"stock_locations",
		"inventory_search_view",
	})

	suite.createStockLocation()
}

func (suite *InventoryServiceTestSuite) Test_CreateStockItem() {
	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-CREATION"}
	resp, err := suite.service.CreateStockItem(stockItem)
	suite.assert.Nil(err)
	suite.assert.Equal(stockItem.SKU, resp.SKU)
}

func (suite *InventoryServiceTestSuite) Test_CreateStockItem_SummaryCreation() {

	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-SUMMARY-CREATED"}
	resp, err := suite.service.CreateStockItem(stockItem)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	summary := []*models.StockItemSummary{}
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&summary).Error
	suite.assert.Nil(err)
	suite.assert.Equal(4, len(summary))
}

func (suite *InventoryServiceTestSuite) Test_GetStockItemById() {
	stockItem := &models.StockItem{StockLocationID: 1, SKU: "TEST-FIND"}
	resp, err := suite.service.CreateStockItem(stockItem)
	suite.assert.Nil(err)
	item, err := suite.service.GetStockItemById(resp.ID)
	suite.assert.Nil(err)
	suite.assert.Equal("TEST-FIND", item.SKU)
}

func (suite *InventoryServiceTestSuite) Test_CreateStockItem_EmptySKU() {
	stockItem := &models.StockItem{StockLocationID: 1}
	_, err := suite.service.CreateStockItem(stockItem)

	suite.assert.NotNil(err)
}

func (suite *InventoryServiceTestSuite) Test_CreateExistingStockItem() {
	stockItem := &models.StockItem{
		StockLocationID: 1,
		SKU:             "TEST-UPSERT",
		DefaultUnitCost: 999,
	}

	resp, err := suite.service.CreateStockItem(stockItem)
	suite.assert.Nil(err)

	var item models.StockItem
	err = suite.db.First(&item, resp.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(stockItem.DefaultUnitCost, item.DefaultUnitCost)

	stockItemUpdate := &models.StockItem{
		StockLocationID: 1,
		SKU:             "TEST-UPSERT",
		DefaultUnitCost: 599,
	}

	respUpdate, err := suite.service.CreateStockItem(stockItemUpdate)
	suite.assert.Nil(err)
	suite.assert.Equal(resp.ID, respUpdate.ID)
	suite.assert.Equal(resp.SKU, respUpdate.SKU)

	var itemUpdate models.StockItem
	err = suite.db.First(&itemUpdate, respUpdate.ID).Error
	suite.assert.Nil(err)
	suite.assert.Equal(stockItemUpdate.DefaultUnitCost, itemUpdate.DefaultUnitCost)
}

func (suite *InventoryServiceTestSuite) Test_IncrementStockItemUnits() {
	resp, err := suite.createStockItem("TEST-INCREMENT", 1)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	suite.assert.Nil(err)
	suite.assert.Equal(1, len(units))
}

func (suite *InventoryServiceTestSuite) Test_IncrementStockItemUnits_MultipleItems() {
	resp, err := suite.createStockItem("TEST-INCREMENT", 10)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	suite.assert.Nil(err)
	suite.assert.Equal(10, len(units))
}

func (suite *InventoryServiceTestSuite) Test_DecrementStockItemUnits() {
	resp, err := suite.createStockItem("TEST-DECREMENT", 10)

	err = suite.service.DecrementStockItemUnits(resp.ID, models.Sellable, 7)
	suite.assert.Nil(err)

	var units []models.StockItemUnit
	err = suite.db.Where("stock_item_id = ?", resp.ID).Find(&units).Error
	suite.assert.Nil(err)
	suite.assert.Equal(3, len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_SingleSKU() {
	resp, err := suite.createStockItem("TEST-RESERVATION", 1)

	refNum := "BR10001"
	skus := map[string]int{"TEST-RESERVATION": 1}

	err = suite.service.ReserveItems(refNum, skus)
	suite.assert.Nil(err)

	var units []models.StockItemUnit
	err = suite.db.Where("ref_num = ?", refNum).Find(&units).Error
	suite.assert.Nil(err)
	suite.assert.Equal(1, len(units))

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	// check if StockItemSummary.Reserved got updated for updated StockItem
	var summary models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	suite.assert.Equal(len(skus), summary.OnHold)
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_MultipleSKUs() {
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
	suite.assert.Nil(err)

	var units []models.StockItemUnit
	err = suite.db.
		Where("ref_num = ?", refNum).
		Where("stock_item_id = ?", resp1.ID).
		Find(&units).Error
	suite.assert.Nil(err)
	suite.assert.Equal(5, len(units))

	err = suite.db.
		Where("ref_num = ?", refNum).
		Where("stock_item_id = ?", resp2.ID).
		Find(&units).Error
	suite.assert.Nil(err)
	suite.assert.Equal(skus[sku1], len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_StockItemChanged() {
	sku := "TEST-RESERVATION"
	suite.createStockItem(sku, 10)

	refNum := "BR10001"
	skus := map[string]int{sku: 1}

	suite.service.ReserveItems(refNum, skus)

	var units []models.StockItemUnit
	err := suite.db.Where("ref_num = ?", refNum).Find(&units).Error
	suite.assert.Nil(err)
	suite.assert.Equal(int(skus[sku]), len(units))
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_NoOnHand() {
	refNum := "BR10001"
	skus := map[string]int{"TEST-DEFAULT": 1}

	err := suite.service.ReserveItems(refNum, skus)
	suite.assert.NotNil(err)
}

func (suite *InventoryServiceTestSuite) Test_ReserveItems_NoSKU() {
	refNum := "BR10001"
	skus := map[string]int{"NO-SKU": 1}

	err := suite.service.ReserveItems(refNum, skus)
	suite.assert.NotNil(err)
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_MultipleSKUsSummary() {
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
	suite.assert.Equal(skus[sku1], summary1.OnHold)

	var summary2 models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp2.ID).First(&summary2)
	suite.assert.Equal(skus[sku2], summary2.OnHold)
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_SubsequentSummary() {
	sku := "TEST-RESERVATION-A"
	resp, _ := suite.createStockItem(sku, 10)

	skus := map[string]int{sku: 3}

	suite.service.ReserveItems("BR10001", skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	var summary models.StockItemSummary
	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	suite.assert.Equal(skus[sku], summary.OnHold)

	skus[sku] = 5

	suite.service.ReserveItems("BR10002", skus)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	suite.db.Where("stock_item_id = ?", resp.ID).First(&summary)
	suite.assert.Equal(8, summary.OnHold)
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_NoReservedSKUs() {
	suite.createStockItem("TEST-RESERVATION-A", 1)

	err := suite.service.ReleaseItems("BR10001")
	suite.assert.NotNil(err, "Should not be able to unreserve items while there are no reservations")
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_Single() {
	skus := []string{"TEST-UNRESERVATION-A"}
	refNum := "BR10001"
	err := suite.createReservation(skus, 1, refNum)
	suite.assert.Nil(err)

	onHoldUnitsCount := 0
	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", refNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.assert.Equal(1, onHoldUnitsCount, "There should be one unit in onHold status")

	// send release request and check if it was processed successfully
	err = suite.service.ReleaseItems(refNum)
	suite.assert.Nil(err, "Reservation should be successfully removed")

	suite.db.Model(&models.StockItemUnit{}).Where("ref_num = ? AND status = ?", refNum, models.StatusOnHold).Count(&onHoldUnitsCount)
	suite.assert.Equal(0, onHoldUnitsCount, "There should not be units in onHold status")
}

func (suite *InventoryServiceTestSuite) Test_ReleaseItems_Summary() {
	skus := []string{"TEST-UNRESERVATION-A"}
	refNum := "BR10001"
	reservedCount := 1
	err := suite.createReservation(skus, reservedCount, refNum)
	suite.assert.Nil(err)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	var summary models.StockItemSummary
	suite.db.Where("type = ?", models.Sellable).First(&summary)

	suite.assert.Equal(reservedCount, summary.OnHold, "One stock item unit should be onHold")

	suite.service.ReleaseItems(refNum)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	suite.db.Where("type = ?", models.Sellable).First(&summary)
	suite.assert.Equal(0, summary.OnHold, "No stock item units should be onHold")
}

func (suite *InventoryServiceTestSuite) Test_GetAFS() {
	resp, err := suite.createStockItem("TEST-DECREMENT", 10)

	// workaround for summary goroutines
	time.Sleep(100 * time.Millisecond)

	afs, err := suite.service.GetAFS(resp.ID, models.Sellable)

	suite.assert.Nil(err)
	suite.assert.Equal(resp.ID, afs.StockItemID)
	suite.assert.Equal(resp.SKU, afs.SKU)
	suite.assert.Equal(10, afs.AFS)
}

func (suite *InventoryServiceTestSuite) Test_GetAFS_NotFound() {
	suite.createStockItem("TEST-DECREMENT", 10)

	afs, err := suite.service.GetAFS(uint(222), models.Sellable)

	suite.assert.Equal(gorm.ErrRecordNotFound, err)
	suite.assert.Nil(afs)
}
