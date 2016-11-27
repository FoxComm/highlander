package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type stockLocationService struct {
	repository repositories.IStockLocationRepository
}

type IStockLocationService interface {
	GetLocations() ([]*models.StockLocation, exceptions.IException)
	GetLocationByID(id uint) (*models.StockLocation, exceptions.IException)
	CreateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException)
	UpdateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException)
	DeleteLocation(id uint) exceptions.IException
}

func NewStockLocationService(repository repositories.IStockLocationRepository) IStockLocationService {
	return &stockLocationService{repository}
}

func (service *stockLocationService) GetLocations() ([]*models.StockLocation, exceptions.IException) {
	return service.repository.GetLocations()
}

func (service *stockLocationService) GetLocationByID(id uint) (*models.StockLocation, exceptions.IException) {
	return service.repository.GetLocationByID(id)
}

func (service *stockLocationService) CreateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	return service.repository.CreateLocation(location)
}

func (service *stockLocationService) UpdateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	return service.repository.UpdateLocation(location)
}

func (service *stockLocationService) DeleteLocation(id uint) exceptions.IException {
	return service.repository.DeleteLocation(id)
}
