package repositories

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
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
	GetLocations() ([]*models.StockLocation, failures.Failure)
	GetLocationByID(id uint) (*models.StockLocation, failures.Failure)
	CreateLocation(location *models.StockLocation) failures.Failure
	UpdateLocation(location *models.StockLocation) failures.Failure
	DeleteLocation(id uint) failures.Failure
}

func NewStockLocationRepository(db *gorm.DB) IStockLocationRepository {
	return &stockLocationRepository{db}
}

func (repository *stockLocationRepository) GetLocations() ([]*models.StockLocation, failures.Failure) {
	locations := []*models.StockLocation{}

	err := repository.db.Find(&locations).Error

	return locations, failures.NewFailure(err)
}

func (repository *stockLocationRepository) GetLocationByID(id uint) (*models.StockLocation, failures.Failure) {
	location := &models.StockLocation{}

	if err := repository.db.First(location, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, failures.NewNotFound("Stock location", id)
		}

		return nil, failures.NewFailure(err)
	}

	return location, nil
}

func (repository *stockLocationRepository) CreateLocation(location *models.StockLocation) failures.Failure {
	if err := repository.db.Create(location).Error; err != nil {
		return failures.NewFailure(err)
	}

	return nil
}

func (repository *stockLocationRepository) UpdateLocation(location *models.StockLocation) failures.Failure {
	res := repository.db.Model(&location).Updates(location)

	if res.Error != nil {
		return failures.NewFailure(res.Error)
	}

	if res.RowsAffected == 0 {
		return failures.NewNotFound("Stock location", location.ID)
	}

	return nil
}

func (repository *stockLocationRepository) DeleteLocation(id uint) failures.Failure {
	res := repository.db.Delete(&models.StockLocation{}, id)

	if res.Error != nil {
		return failures.NewFailure(res.Error)
	}

	if res.RowsAffected == 0 {
		return failures.NewNotFound("Stock location", id)
	}

	return nil
}
