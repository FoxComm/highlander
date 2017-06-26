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

func (s *skuService) upsertSku(txn *gorm.DB, sku *models.SKU) error {
	onConflict := `ON CONFLICT (code) DO UPDATE SET 
				scope = EXCLUDED.scope, 
				upc = EXCLUDED.upc, 
				title=EXCLUDED.title, 
				unit_cost_currency=EXCLUDED.unit_cost_currency, 
				unit_cost_value=EXCLUDED.unit_cost_value,
				tax_class=EXCLUDED.tax_class,
				requires_shipping=EXCLUDED.requires_shipping,
				shipping_class=EXCLUDED.shipping_class,
				is_returnable=EXCLUDED.is_returnable,
				return_window_value=EXCLUDED.return_window_value,
				return_window_units=EXCLUDED.return_window_units,
				height_value=EXCLUDED.height_value,
				height_units=EXCLUDED.height_units,
				weight_value=EXCLUDED.weight_value,
				weight_units=EXCLUDED.length_value,
				length_value=EXCLUDED.length_value,
				length_units=EXCLUDED.length_units,
				width_value=EXCLUDED.width_value,
				width_units=EXCLUDED.width_units,
				requires_inventory_tracking=EXCLUDED.requires_inventory_tracking,
				inventory_warning_level_is_enabled=EXCLUDED.inventory_warning_level_is_enabled,
				inventory_warning_level_value=EXCLUDED.inventory_warning_level_value,
				maximum_quantity_in_cart_value=EXCLUDED.maximum_quantity_in_cart_value,
				maximum_quantity_in_cart_is_enabled=EXCLUDED.maximum_quantity_in_cart_is_enabled,
				minimum_quantity_in_cart_value=EXCLUDED.minimum_quantity_in_cart_value,
				minimum_quantity_in_cart_is_enabled=EXCLUDED.minimum_quantity_in_cart_is_enabled,
				allow_backorder=EXCLUDED.allow_backorder,
				allow_preorder=EXCLUDED.allow_preorder,
				requires_lot_tracking=EXCLUDED.requires_lot_tracking,
				lot_expiration_threshold_value=EXCLUDED.lot_expiration_threshold_value,
				lot_expiration_threshold_units=EXCLUDED.lot_expiration_threshold_units,
				lot_expiration_warning_threshold_value=EXCLUDED.lot_expiration_warning_threshold_value,
				lot_expiration_warning_threshold_units=EXCLUDED.lot_expiration_warning_threshold_units`

	if err := txn.Set("gorm:insert_option", onConflict).Create(sku).Error; err != nil {
		return err
	}

	return nil
}

func (s *skuService) createInner(txn *gorm.DB, payload *payloads.CreateSKU) (*responses.SKU, error) {
	sku := payload.Model()

	if err := sku.Validate(); err != nil {
		return nil, err
	}

	if err := s.upsertSku(txn, sku); err != nil {
		return nil, err
	}

	if sku.RequiresInventoryTracking {
		// By default, create a stock item in each existing stock location.
		stockLocationRepo := repositories.NewStockLocationRepository(txn)
		locations, err := stockLocationRepo.GetLocations()
		if err != nil {
			return nil, err
		}

		stockItemRepo := repositories.NewStockItemRepository(txn)
		for _, location := range locations {
			stockItem := models.StockItem{
				SKU:             sku.Code,
				StockLocationID: location.ID,
				DefaultUnitCost: sku.UnitCostValue,
			}

			if err := stockItemRepo.UpsertStockItem(&stockItem); err != nil {
				return nil, err
			}

			summaryService := NewSummaryService(txn)
			if err := summaryService.CreateStockItemSummary(stockItem.ID); err != nil {
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
