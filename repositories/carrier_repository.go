package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type carrierRepository struct {
	db *gorm.DB
}

type ICarrierRepository interface {
	GetCarriers() ([]*models.Carrier, error)
	GetCarrierByID(id uint) (*models.Carrier, error)
	CreateCarrier(carrier *models.Carrier) (uint, error)
	UpdateCarrier(carrier *models.Carrier) error
	DeleteCarrier(id uint) error
}

func NewCarrierRepository(db *gorm.DB) ICarrierRepository {
	return &carrierRepository{db}
}

func (repository *carrierRepository) GetCarriers() ([]*models.Carrier, error) {
	var data []models.Carrier
	if err := repository.db.Find(&data).Error; err != nil {
		return nil, err
	}

	carriers := make([]*models.Carrier, len(data))
	for i := range data {
		carriers[i] = &data[i]
	}

	return carriers, nil
}

func (repository *carrierRepository) GetCarrierByID(id uint) (*models.Carrier, error) {
	var carrier models.Carrier
	if err := repository.db.First(&carrier, id).Error; err != nil {
		return nil, err
	}

	return &carrier, nil
}

func (repository *carrierRepository) CreateCarrier(carrier *models.Carrier) (uint, error) {
	err := repository.db.Create(carrier).Error

	return carrier.ID, err
}

func (repository *carrierRepository) UpdateCarrier(carrier *models.Carrier) error {
	result := repository.db.Model(&carrier).Updates(carrier)

	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}

	return result.Error
}

func (repository *carrierRepository) DeleteCarrier(id uint) error {
	carrier := models.Carrier{ID: id}

	result := repository.db.Delete(&carrier)

	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}

	return result.Error
}
