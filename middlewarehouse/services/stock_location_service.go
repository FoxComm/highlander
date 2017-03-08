package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type stockLocationService struct {
	repository repositories.IStockLocationRepository
}

type StockLocationService interface {
	GetLocations() ([]*models.StockLocation, error)
	GetLocationByID(id uint) (*models.StockLocation, error)
	CreateLocation(location *models.StockLocation) (*models.StockLocation, error)
	UpdateLocation(location *models.StockLocation) (*models.StockLocation, error)
	DeleteLocation(id uint) error
}

func NewStockLocationService(db *gorm.DB) StockLocationService {
	repository := repositories.NewStockLocationRepository(db)
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
