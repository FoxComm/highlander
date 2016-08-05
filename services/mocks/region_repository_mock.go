package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type RegionRepositoryMock struct {
	mock.Mock
}

func (service *RegionRepositoryMock) GetRegionByID(id uint) (*models.Region, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Region); ok {
		return model, nil
	}

	return nil, args.Error(1)
}
