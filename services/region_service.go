package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type regionService struct {
	repository repositories.IRegionRepository
}

type IRegionService interface {
	GetRegionByID(id uint) (*models.Region, error)
}

func NewRegionService(repository repositories.IRegionRepository) IRegionService {
	return &regionService{repository}
}

func (service *regionService) GetRegionByID(id uint) (*models.Region, error) {
	return service.repository.GetRegionByID(id)
}
