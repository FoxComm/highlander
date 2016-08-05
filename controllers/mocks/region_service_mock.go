package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type RegionServiceMock struct {
	mock.Mock
}

func (service *RegionServiceMock) GetRegionByID(id uint) (*models.Region, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Region); ok {
		return model, nil
	}

	return nil, args.Error(1)
}
