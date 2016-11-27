package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type ActivityLoggerMock struct{}

func (logger *ActivityLoggerMock) Log(activity activities.ISiteActivity) exceptions.IException {
	return nil
}
