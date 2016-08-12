package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
)

type addressService struct {
	repository repositories.IAddressRepository
}

type IAddressService interface {
	GetAddressByID(id uint) (*models.Address, error)
	CreateAddress(address *models.Address) (*models.Address, error)
	DeleteAddress(id uint) error
}

func NewAddressService(repository repositories.IAddressRepository) IAddressService {
	return &addressService{repository}
}

func (service *addressService) GetAddressByID(id uint) (*models.Address, error) {
	return service.repository.GetAddressByID(id)
}

func (service *addressService) CreateAddress(address *models.Address) (*models.Address, error) {
	return service.repository.CreateAddress(address)
}

func (service *addressService) DeleteAddress(id uint) error {
	return service.repository.DeleteAddress(id)
}
