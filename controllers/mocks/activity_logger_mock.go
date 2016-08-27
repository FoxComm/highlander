package mocks

import "github.com/FoxComm/middlewarehouse/models/activities"

type ActivityLoggerMock struct{}

func (logger *ActivityLoggerMock) Log(activity activities.SiteActivity) error {
	return nil
}
