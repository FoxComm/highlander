package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
<<<<<<< Updated upstream
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
=======

	"github.com/jinzhu/gorm"
>>>>>>> Stashed changes
)

type carrierService struct {
	repository repositories.ICarrierRepository
}

type ICarrierService interface {
	GetCarriers() ([]*models.Carrier, error)
	GetCarrierByID(id uint) (*models.Carrier, error)
	CreateCarrier(carrier *models.Carrier) (*models.Carrier, error)
	UpdateCarrier(carrier *models.Carrier) (*models.Carrier, error)
	DeleteCarrier(id uint) error
}

func NewCarrierService(repository repositories.ICarrierRepository) ICarrierService {
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
