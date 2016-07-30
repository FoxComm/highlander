package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
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

	if err := repository.db.Find(&locations).Error; err != nil {
		return nil, err
	}

	return locations, nil
}

func (repository *stockLocationRepository) GetLocationByID(id uint) (*models.StockLocation, error) {
	location := &models.StockLocation{}

	if err := repository.db.First(location, id).Error; err != nil {
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
		return nil, gorm.ErrRecordNotFound
	}

	return repository.GetLocationByID(location.ID)
}

func (repository *stockLocationRepository) DeleteLocation(id uint) error {
	res := repository.db.Delete(&models.StockLocation{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}

	return nil
}
