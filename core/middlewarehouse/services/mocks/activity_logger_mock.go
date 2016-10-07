package mocks

import "github.com/FoxComm/highlander/middlewarehouse/models/activities"

type ActivityLoggerMock struct{}

func (logger *ActivityLoggerMock) Log(activity activities.ISiteActivity) error {
	return nil
}
