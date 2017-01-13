package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type carrierService struct {
	repository repositories.ICarrierRepository
}

type CarrierService interface {
	GetCarriers() ([]*models.Carrier, error)
	GetCarrierByID(id uint) (*models.Carrier, error)
	CreateCarrier(carrier *models.Carrier) (*models.Carrier, error)
	UpdateCarrier(carrier *models.Carrier) (*models.Carrier, error)
	DeleteCarrier(id uint) error
}

func NewCarrierService(db *gorm.DB) CarrierService {
	repository := repositories.NewCarrierRepository(db)
	return &carrierService{repository}
}

func (service *carrierService) GetCarriers() ([]*models.Carrier, error) {
	return service.repository.GetCarriers()
}

func (service *carrierService) GetCarrierByID(id uint) (*models.Carrier, error) {
	return service.repository.GetCarrierByID(id)
}

func (service *carrierService) CreateCarrier(carrier *models.Carrier) (*models.Carrier, error) {
	return service.repository.CreateCarrier(carrier)
}

func (service *carrierService) UpdateCarrier(carrier *models.Carrier) (*models.Carrier, error) {
	return service.repository.UpdateCarrier(carrier)
}

func (service *carrierService) DeleteCarrier(id uint) error {
	return service.repository.DeleteCarrier(id)
}
