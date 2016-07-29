package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type IAddressRepository interface {
	CreateAddress(*models.Address) (*models.Address, error)
	GetAddressByID(id uint) (*models.Address, error)
}

type addressRepository struct {
	db *gorm.DB
}

func NewAddressRepository(db *gorm.DB) IAddressRepository {
	return &addressRepository{db}
}

func (repository *addressRepository) GetAddressByID(id uint) (*models.Address, error) {
	var address models.Address

	if err := repository.db.First(&address, id).Error; err != nil {
		return nil, err
	}

	return &address, nil
}

func (repository *addressRepository) CreateAddress(address *models.Address) (*models.Address, error) {
	err := repository.db.Create(address).Error

	if err != nil {
		return nil, err
	}

	return repository.GetAddressByID(address.ID)
}
