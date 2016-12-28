package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/stretchr/testify/mock"
)

type ActivityLoggerMock struct {
	mock.Mock
}

func (logger *ActivityLoggerMock) Log(activity activities.ISiteActivity) error {
	args := logger.Called(activity)

	return args.Error(0)
}
