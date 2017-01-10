package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type stockLocationService struct {
	repository repositories.IStockLocationRepository
}

type StockLocationService interface {
	GetLocations() ([]*models.StockLocation, failures.Failure)
	GetLocationByID(id uint) (*models.StockLocation, failures.Failure)
	CreateLocation(location *models.StockLocation) (*models.StockLocation, failures.Failure)
	UpdateLocation(location *models.StockLocation) (*models.StockLocation, failures.Failure)
	DeleteLocation(id uint) failures.Failure
}

func NewStockLocationService(db *gorm.DB) StockLocationService {
	repository := repositories.NewStockLocationRepository(db)
	return &stockLocationService{repository}
}

func (service *stockLocationService) GetLocations() ([]*models.StockLocation, failures.Failure) {
	return service.repository.GetLocations()
}

func (service *stockLocationService) GetLocationByID(id uint) (*models.StockLocation, failures.Failure) {
	return service.repository.GetLocationByID(id)
}

func (service *stockLocationService) CreateLocation(location *models.StockLocation) (*models.StockLocation, failures.Failure) {
	failure := service.repository.CreateLocation(location)
	return location, failure
}

func (service *stockLocationService) UpdateLocation(location *models.StockLocation) (*models.StockLocation, failures.Failure) {
	failure := service.repository.UpdateLocation(location)
	return location, failure
}

func (service *stockLocationService) DeleteLocation(id uint) failures.Failure {
	return service.repository.DeleteLocation(id)
}
