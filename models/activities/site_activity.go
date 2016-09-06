package activities

import "time"

// SiteActivity is as action that occurs within the system that should be
// written to the activity log.
type ISiteActivity interface {
	Type() string
	Data() string
	CreatedAt() string
}

type defaultSiteActivity struct {
	data         string
	activityType string
	createdAt    time.Time
}

func (a defaultSiteActivity) Data() string {
	return a.data
}

func (a defaultSiteActivity) Type() string {
	return a.activityType
}

func (a defaultSiteActivity) CreatedAt() string {
	return a.createdAt.String()
}
