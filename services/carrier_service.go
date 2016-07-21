package services

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type carrierService struct {
	db *gorm.DB
}

type ICarrierService interface {
	GetCarriers() ([]*models.Carrier, error)
	GetCarrierByID(id uint) (*models.Carrier, error)
	CreateCarrier(payload *payloads.Carrier) (uint, error)
	UpdateCarrier(id uint, payload *payloads.Carrier) error
	DeleteCarrier(id uint) error
}

func NewCarrierService(db *gorm.DB) ICarrierService {
	return &carrierService{db}
}

func (service *carrierService) GetCarriers() ([]*models.Carrier, error) {
	var data []models.Carrier
	if err := service.db.Find(&data).Error; err != nil {
		return nil, err
	}

	carriers := make([]*models.Carrier, len(data))
	for i := range data {
		carriers[i] = &data[i]
	}

	return carriers, nil
}

func (service *carrierService) GetCarrierByID(id uint) (*models.Carrier, error) {
	var carrier models.Carrier
	if err := service.db.First(&carrier, id).Error; err != nil {
		return nil, err
	}

	return &carrier, nil
}

func (service *carrierService) CreateCarrier(payload *payloads.Carrier) (uint, error) {
	carrier := models.NewCarrierFromPayload(payload)

	err := service.db.Create(carrier).Error

	return carrier.ID, err
}

func (service *carrierService) UpdateCarrier(id uint, payload *payloads.Carrier) error {

	carrier := models.NewCarrierFromPayload(payload)
	carrier.ID = id

	return service.db.Model(&carrier).Updates(carrier).Error
}

func (service *carrierService) DeleteCarrier(id uint) error {
	carrier := models.Carrier{ID: id}

	return service.db.Delete(&carrier).Error
}
