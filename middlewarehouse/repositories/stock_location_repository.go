package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorStockLocationNotFound = "Stock location with id=%d not found"
)

type stockLocationRepository struct {
	db *gorm.DB
}

type IStockLocationRepository interface {
	GetLocations() ([]*models.StockLocation, error)
	GetLocationByID(id uint) (*models.StockLocation, error)
	CreateLocation(location *models.StockLocation) (*models.StockLocation, error)
	UpdateLocation(location *models.StockLocation) (*models.StockLocation, error)
	DeleteLocation(id uint) error
}

func NewStockLocationRepository(db *gorm.DB) IStockLocationRepository {
	return &stockLocationRepository{db}
}

func (repository *stockLocationRepository) GetLocations() ([]*models.StockLocation, error) {
	locations := []*models.StockLocation{}

	err := repository.db.
		Preload("Address").
		Preload("Address.Region").
		Preload("Address.Region.Country").
		Find(&locations).Error

	return locations, err
}

func (repository *stockLocationRepository) GetLocationByID(id uint) (*models.StockLocation, error) {
	location := &models.StockLocation{}

	err := repository.db.
		Preload("Address").
		Preload("Address.Region").
		Preload("Address.Region.Country").
		First(location, id).Error

	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorStockLocationNotFound, id)
		}
		return nil, err
	}

	return location, nil
}

func (repository *stockLocationRepository) CreateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	err := repository.db.Create(location).Error

	if err != nil {
		return nil, err
	}

	return repository.GetLocationByID(location.ID)
}

func (repository *stockLocationRepository) UpdateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	res := repository.db.Model(&location).Updates(location)

	if res.Error != nil {
		return nil, res.Error
	}

	if res.RowsAffected == 0 {
		return nil, fmt.Errorf(ErrorStockLocationNotFound, location.ID)
	}

	return repository.GetLocationByID(location.ID)
}

func (repository *stockLocationRepository) DeleteLocation(id uint) error {
	res := repository.db.Delete(&models.StockLocation{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return fmt.Errorf(ErrorStockLocationNotFound, id)
	}

	return nil
}
