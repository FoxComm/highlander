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
	Get() ([]*models.Carrier, error)
	GetByID(id uint) (*models.Carrier, error)
	Create(payload *payloads.Carrier) (uint, error)
	Update(id uint, payload *payloads.Carrier) error
	Delete(id uint) error
}

func NewCarrierService(db *gorm.DB) ICarrierService {
	return &carrierService{db}
}

func (service *carrierService) Get() ([]*models.Carrier, error) {
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

func (service *carrierService) GetByID(id uint) (*models.Carrier, error) {
	var carrier models.Carrier
	if err := service.db.First(&carrier, id).Error; err != nil {
		return nil, err
	}

	return &carrier, nil
}

func (service *carrierService) Create(payload *payloads.Carrier) (uint, error) {
	carrier := models.NewCarrierFromPayload(payload)

	err := service.db.Create(carrier).Error

	return carrier.ID, err
}

func (service *carrierService) Update(id uint, payload *payloads.Carrier) error {

	carrier := models.NewCarrierFromPayload(payload)
	carrier.ID = id

	return service.db.Model(&carrier).Updates(carrier).Error
}

func (service *carrierService) Delete(id uint) error {
	carrier := models.Carrier{ID: id}

	return service.db.Delete(&carrier).Error
}
