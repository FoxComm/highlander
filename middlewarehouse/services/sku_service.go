package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type SKU interface {
	GetByID(id uint) (*responses.SKU, error)
	Create(payload *payloads.CreateSKU) (*responses.SKU, error)
	Update(id uint, payload *payloads.UpdateSKU) (*responses.SKU, error)
	Delete(id uint) error
}

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

func (s *skuService) Create(payload *payloads.CreateSKU) (*responses.SKU, error) {
	sku := payload.Model()

	if err := s.db.Create(sku).Error; err != nil {
		return nil, err
	}

	return responses.NewSKUFromModel(sku), nil
}

func (s *skuService) Update(id uint, payload *payloads.UpdateSKU) (*responses.SKU, error) {
	tx := s.db.Begin()

	sku := new(models.SKU)
	if err := tx.Find(sku, id).Error; err != nil {
		tx.Rollback()
		return nil, err
	}

	updated := payload.Model(sku)
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

func (s *skuService) Delete(id uint) error {
	sku := models.SKU{}
	sku.ID = id

	return s.db.Delete(&sku).Error
}
