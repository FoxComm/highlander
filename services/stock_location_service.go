package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type stockLocationService struct {
	repository repositories.IStockLocationRepository
}

type IStockLocationService interface {
	GetLocations() ([]*models.StockLocation, error)
	GetLocationByID(id uint) (*models.StockLocation, error)
	CreateLocation(location *models.StockLocation) (*models.StockLocation, error)
	UpdateLocation(location *models.StockLocation) (*models.StockLocation, error)
	DeleteLocation(id uint) error
}

func NewStockLocationService(repository repositories.IStockLocationRepository) IStockLocationService {
	return &stockLocationService{repository}
}

func (service *stockLocationService) GetLocations() ([]*models.StockLocation, error) {
	return service.repository.GetLocations()
}

func (service *stockLocationService) GetLocationByID(id uint) (*models.StockLocation, error) {
	return service.repository.GetLocationByID(id)
}

func (service *stockLocationService) CreateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	return service.repository.CreateLocation(location)
}

func (service *stockLocationService) UpdateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	return service.repository.UpdateLocation(location)
}

func (service *stockLocationService) DeleteLocation(id uint) error {
	return service.repository.DeleteLocation(id)
}
