package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type carrierService struct {
	repository repositories.ICarrierRepository
}

type ICarrierService interface {
	GetCarriers() ([]*models.Carrier, exceptions.IException)
	GetCarrierByID(id uint) (*models.Carrier, exceptions.IException)
	CreateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException)
	UpdateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException)
	DeleteCarrier(id uint) exceptions.IException
}

func NewCarrierService(repository repositories.ICarrierRepository) ICarrierService {
	return &carrierService{repository}
}

func (service *carrierService) GetCarriers() ([]*models.Carrier, exceptions.IException) {
	return service.repository.GetCarriers()
}

func (service *carrierService) GetCarrierByID(id uint) (*models.Carrier, exceptions.IException) {
	return service.repository.GetCarrierByID(id)
}

func (service *carrierService) CreateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException) {
	return service.repository.CreateCarrier(carrier)
}

func (service *carrierService) UpdateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException) {
	return service.repository.UpdateCarrier(carrier)
}

func (service *carrierService) DeleteCarrier(id uint) exceptions.IException {
	return service.repository.DeleteCarrier(id)
}
