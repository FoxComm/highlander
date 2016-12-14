package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type SKU interface {
	Create(payload *payloads.CreateSKU) (*responses.SKU, error)
	GetByID(id uint) (*responses.SKU, error)
}

func NewSKU(db *gorm.DB) SKU {
	return &skuService{db}
}

type skuService struct {
	db *gorm.DB
}

func (s *skuService) Create(payload *payloads.CreateSKU) (*responses.SKU, error) {
	sku := payload.Model()

	if err := s.db.Create(sku).Error; err != nil {
		return nil, err
	}

	return responses.NewSKUFromModel(sku), nil
}

func (s *skuService) GetByID(id uint) (*responses.SKU, error) {
	sku := new(models.SKU)

	if err := s.db.First(sku, id).Error; err != nil {
		return nil, err
	}

	return responses.NewSKUFromModel(sku), nil
}
