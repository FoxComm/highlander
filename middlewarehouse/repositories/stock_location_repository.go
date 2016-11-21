package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
	"strconv"
)

const (
	ErrorStockLocationNotFound = "Stock location with id=%d not found"
	StockLocationEntity        = "stockLocation"
)

type stockLocationRepository struct {
	db *gorm.DB
}

type IStockLocationRepository interface {
	GetLocations() ([]*models.StockLocation, exceptions.IException)
	GetLocationByID(id uint) (*models.StockLocation, exceptions.IException)
	CreateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException)
	UpdateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException)
	DeleteLocation(id uint) exceptions.IException
}

func NewStockLocationRepository(db *gorm.DB) IStockLocationRepository {
	return &stockLocationRepository{db}
}

func (repository *stockLocationRepository) GetLocations() ([]*models.StockLocation, exceptions.IException) {
	locations := []*models.StockLocation{}

	err := repository.db.Find(&locations).Error

	return locations, NewDatabaseException(err)
}

func (repository *stockLocationRepository) GetLocationByID(id uint) (*models.StockLocation, exceptions.IException) {
	location := &models.StockLocation{}

	if err := repository.db.First(location, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, NewEntityNotFoundException(StockLocationEntity, strconv.Itoa(int(id)), fmt.Errorf(ErrorStockLocationNotFound, id))
		}

		return nil, NewDatabaseException(err)
	}

	return location, nil
}

func (repository *stockLocationRepository) CreateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	err := repository.db.Create(location).Error

	if err != nil {
		return nil, NewDatabaseException(err)
	}

	return repository.GetLocationByID(location.ID)
}

func (repository *stockLocationRepository) UpdateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	res := repository.db.Model(&location).Updates(location)

	if res.Error != nil {
		return nil, NewDatabaseException(res.Error)
	}

	if res.RowsAffected == 0 {
		return nil, NewEntityNotFoundException(StockLocationEntity, strconv.Itoa(int(location.ID)), fmt.Errorf(ErrorStockLocationNotFound, location.ID))
	}

	return repository.GetLocationByID(location.ID)
}

func (repository *stockLocationRepository) DeleteLocation(id uint) exceptions.IException {
	res := repository.db.Delete(&models.StockLocation{}, id)

	if res.Error != nil {
		return NewDatabaseException(res.Error)
	}

	if res.RowsAffected == 0 {
		return NewEntityNotFoundException(StockLocationEntity, strconv.Itoa(int(id)), fmt.Errorf(ErrorStockLocationNotFound, id))
	}

	return nil
}
