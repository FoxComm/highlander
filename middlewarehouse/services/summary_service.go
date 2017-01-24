package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
	"log"
	"reflect"
	"strings"
)

type summaryService struct {
	summaryRepo   repositories.ISummaryRepository
	stockItemRepo repositories.IStockItemRepository
	txn           *gorm.DB
}

type SummaryService interface {
	WithTransaction(txn *gorm.DB) SummaryService

	CreateStockItemSummary(stockItemId uint) error
	UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) error

	CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) error

	GetSummary() ([]*models.StockItemSummary, error)
	GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error)
}

func NewSummaryService(db *gorm.DB) SummaryService {
	return &summaryService{
		summaryRepo:   repositories.NewSummaryRepository(db),
		stockItemRepo: repositories.NewStockItemRepository(db),
		txn:           nil,
	}
}

func (service *summaryService) WithTransaction(txn *gorm.DB) SummaryService {
	return &summaryService{
		summaryRepo:   service.summaryRepo,
		stockItemRepo: service.stockItemRepo,
		txn:           txn,
	}
}

func (service *summaryService) GetSummary() ([]*models.StockItemSummary, error) {
	return service.summaryRepo.GetSummary()
}

func (service *summaryService) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error) {
	return service.summaryRepo.GetSummaryBySKU(sku)
}

func (service *summaryService) CreateStockItemSummary(stockItemId uint) error {
	summary := []*models.StockItemSummary{
		{StockItemID: stockItemId, Type: models.Sellable},
		{StockItemID: stockItemId, Type: models.NonSellable},
		{StockItemID: stockItemId, Type: models.Backorder},
		{StockItemID: stockItemId, Type: models.Preorder},
	}

	return service.getSummaryRepo().CreateStockItemSummary(summary)
}

func (service *summaryService) UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) error {
	stockItem, err := service.stockItemRepo.GetStockItemById(stockItemId)
	if err != nil {
		return err
	}

	summary, err := service.getSummaryRepo().GetSummaryItemByType(stockItemId, unitType)
	if err != nil {
		return err
	}

	// changing status from onHand does not affect onHand count, so skip it
	if status.From != models.StatusOnHand {
		summary = updateStatusUnitsAmount(summary, status.From, -qty)
	}

	// update count of .To status if it is new record or it's not moved to onHand status
	// or shipped (because in that case, the stock item unit is deleted).
	if status.From == models.StatusEmpty || status.To != models.StatusOnHand {
		summary = updateStatusUnitsAmount(summary, status.To, qty)
	}

	// decrease OnHand items when item is shipped
	if status.To == models.StatusShipped {
		summary = updateStatusUnitsAmount(summary, models.StatusOnHand, -qty)
	}

	summary = updateAfs(summary, status, qty)
	summary = updateAfsCost(summary, stockItem)

	// update stock item summary values
	if err := service.getSummaryRepo().UpdateStockItemSummary(summary); err != nil {
		log.Printf("Error updating stock_item_summaries with error: %s", err.Error())

		return err
	}

	// create related stock item transaction
	return service.CreateStockItemTransaction(summary, status.To, qty)
}

func (service *summaryService) CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) error {
	// If order was shipped create transaction for onHand items - delete qty items
	if status == models.StatusShipped {
		status = models.StatusOnHand
		qty = -qty
	}

	transaction := &models.StockItemTransaction{
		StockItemId:    summary.StockItemID,
		Type:           summary.Type,
		Status:         status,
		QuantityNew:    getStatusAmountChange(summary, status),
		QuantityChange: qty,
		AFSNew:         uint(summary.AFS),
	}

	if err := service.getSummaryRepo().CreateStockItemTransaction(transaction); err != nil {
		log.Printf("Error creating stock_item_transactions with error: %s", err.Error())
	}

	return nil
}

func updateStatusUnitsAmount(summary *models.StockItemSummary, status models.UnitStatus, qty int) *models.StockItemSummary {
	if status == models.StatusEmpty {
		return summary
	}

	switch status {
	case models.StatusOnHand:
		summary.OnHand += qty
	case models.StatusOnHold:
		summary.OnHold += qty
	case models.StatusReserved:
		summary.Reserved += qty
	case models.StatusShipped:
		summary.Shipped += qty
	}

	return summary
}

func updateAfs(summary *models.StockItemSummary, shift models.StatusChange, qty int) *models.StockItemSummary {
	if shift.To == models.StatusOnHand {
		summary.AFS += qty
	}

	if shift.From == models.StatusOnHand {
		summary.AFS -= qty
	}

	return summary
}

func updateAfsCost(summary *models.StockItemSummary, stockItem *models.StockItem) *models.StockItemSummary {
	summary.AFSCost = summary.AFS * stockItem.DefaultUnitCost

	return summary
}

func getStatusAmountChange(summary *models.StockItemSummary, status models.UnitStatus) uint {
	field := strings.Title(string(status))
	r := reflect.ValueOf(summary)
	f := reflect.Indirect(r).FieldByName(field)

	return uint(f.Int())
}

func (service *summaryService) getSummaryRepo() repositories.ISummaryRepository {
	if service.txn == nil {
		return service.summaryRepo
	}

	return service.summaryRepo.WithTransaction(service.txn)
}
