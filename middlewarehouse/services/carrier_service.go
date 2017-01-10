package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type carrierService struct {
	repository repositories.ICarrierRepository
}

type CarrierService interface {
	GetCarriers() ([]*models.Carrier, failures.Failure)
	GetCarrierByID(id uint) (*models.Carrier, failures.Failure)
	CreateCarrier(carrier *models.Carrier) (*models.Carrier, failures.Failure)
	UpdateCarrier(carrier *models.Carrier) (*models.Carrier, failures.Failure)
	DeleteCarrier(id uint) failures.Failure
}

func NewCarrierService(db *gorm.DB) CarrierService {
	repository := repositories.NewCarrierRepository(db)
	return &carrierService{repository}
}

func (service *carrierService) GetCarriers() ([]*models.Carrier, failures.Failure) {
	return service.repository.GetCarriers()
}

func (service *carrierService) GetCarrierByID(id uint) (*models.Carrier, failures.Failure) {
	return service.repository.GetCarrierByID(id)
}

func (service *carrierService) CreateCarrier(carrier *models.Carrier) (*models.Carrier, failures.Failure) {
	failure := service.repository.CreateCarrier(carrier)
	return carrier, failure
}

func (service *carrierService) UpdateCarrier(carrier *models.Carrier) (*models.Carrier, failures.Failure) {
	failure := service.repository.UpdateCarrier(carrier)
	return carrier, failure
}

func (service *carrierService) DeleteCarrier(id uint) failures.Failure {
	return service.repository.DeleteCarrier(id)
}
