package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

// SKU is a collection of methods for creating and manipulating SKUs.
type SKU interface {
	GetByID(id uint) (*responses.SKU, error)
	Create(payload *payloads.CreateSKU) (*responses.SKU, error)
	CreateBulk(payload []*payloads.CreateSKU) ([]*responses.SKU, error)
	Update(id uint, payload *payloads.UpdateSKU) (*responses.SKU, error)
	Archive(id uint) error
	GetAFS(id uint) (*responses.SkuAfs, error)
}

// NewSKU creates a new SKU collection.
func NewSKU(db *gorm.DB) SKU {
	return &skuService{db}
}

type skuService struct {
	db *gorm.DB
}

func (s *skuService) GetByID(id uint) (*responses.SKU, error) {
	sku := new(models.SKU)

	if err := s.db.First(sku, id).Error; err != nil {
		return nil, err
	}

	return responses.NewSKUFromModel(sku), nil
}

func (s *skuService) CreateBulk(payloads []*payloads.CreateSKU) ([]*responses.SKU, error) {
	resps := []*responses.SKU{}
	txn := s.db.Begin()

	for _, payload := range payloads {
		resp, err := s.createInner(txn, payload)
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		resps = append(resps, resp)
	}

	if err := txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	return resps, nil
}

func (s *skuService) Create(payload *payloads.CreateSKU) (*responses.SKU, error) {
	txn := s.db.Begin()
	resp, err := s.createInner(txn, payload)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	return resp, nil
}

func (s *skuService) Update(id uint, payload *payloads.UpdateSKU) (*responses.SKU, error) {
	tx := s.db.Begin()

	sku := new(models.SKU)
	if err := tx.Find(sku, id).Error; err != nil {
		tx.Rollback()
		return nil, err
	}

	updated := payload.Model(sku)
	if err := updated.Validate(); err != nil {
		tx.Rollback()
		return nil, err
	}

	if err := tx.Save(updated).Error; err != nil {
		tx.Rollback()
		return nil, err
	}

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return nil, err
	}

	return responses.NewSKUFromModel(updated), nil
}

func (s *skuService) Archive(id uint) error {
	sku := models.SKU{}
	sku.ID = id

	return s.db.Delete(&sku).Error
}

func (s *skuService) createInner(txn *gorm.DB, payload *payloads.CreateSKU) (*responses.SKU, error) {
	sku := payload.Model()

	if err := sku.Validate(); err != nil {
		return nil, err
	}

	if err := txn.Create(sku).Error; err != nil {
		return nil, err
	}

	// By default, create a stock item in each existing stock location.
	stockLocationRepo := repositories.NewStockLocationRepository(txn)
	locations, err := stockLocationRepo.GetLocations()
	if err != nil {
		return nil, err
	}

	if sku.RequiresInventoryTracking {
		stockItemRepo := repositories.NewStockItemRepository(txn)
		for _, location := range locations {
			stockItem := models.StockItem{
				SKU:             sku.Code,
				StockLocationID: location.ID,
				DefaultUnitCost: sku.UnitCostValue,
			}

			createdStockItem, err := stockItemRepo.CreateStockItem(&stockItem)
			if err != nil {
				return nil, err
			}

			summaryService := NewSummaryService(txn)
			if err := summaryService.CreateStockItemSummary(createdStockItem.ID); err != nil {
				return nil, err
			}
		}
	}

	return responses.NewSKUFromModel(sku), nil
}

func (s *skuService) GetAFS(id uint) (*responses.SkuAfs, error) {
	repo := repositories.NewStockItemRepository(s.db)
	afs, err := repo.GetAFSBySkuId(id)
	if err != nil {
		return nil, err
	}
	var resp responses.SkuAfs

	for _, single := range *afs {
		switch single.Type {
		case models.Sellable:
			resp.Sellable = single.Afs
		case models.NonSellable:
			resp.NonSellable = single.Afs
		case models.Backorder:
			resp.Backorder = single.Afs
		case models.Preorder:
			resp.Preorder = single.Afs
		}
	}

	return &resp, nil
}
