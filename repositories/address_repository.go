package repositories

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorAddressNotFound = "Address with id=%d not found"
)

type IAddressRepository interface {
	GetAddressByID(id uint) (*models.Address, error)
	CreateAddress(address *models.Address) (*models.Address, error)
	DeleteAddress(id uint) error
}

type addressRepository struct {
	db *gorm.DB
}

func NewAddressRepository(db *gorm.DB) IAddressRepository {
	return &addressRepository{db}
}

func (repository *addressRepository) GetAddressByID(id uint) (*models.Address, error) {
	address := &models.Address{}

	if err := repository.db.First(address, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorAddressNotFound, id)
		}

		return nil, err
	}

	repository.db.Model(address).Related(&address.Region)
	repository.db.Model(address.Region).Related(&address.Region.Country)

	return address, nil
}

func (repository *addressRepository) CreateAddress(address *models.Address) (*models.Address, error) {
	err := repository.db.Set("gorm:save_associations", false).Create(address).Error

	if err != nil {
		return nil, err
	}

	return repository.GetAddressByID(address.ID)
}

func (repository *addressRepository) DeleteAddress(id uint) error {
	res := repository.db.Delete(&models.Address{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return fmt.Errorf(ErrorAddressNotFound, id)
	}

	return nil
}
