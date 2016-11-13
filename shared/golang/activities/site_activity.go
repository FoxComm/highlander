package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/metamorphosis"
)

// SiteActivity is as action that occurs within the system that should be
// written to the activity log.
type SiteActivity interface {
	Type() string
	Data() string
	CreatedAt() string
}

func NewActivityFromAvro(message metamorphosis.AvroMessage) (SiteActivity, error) {
	a := new(defaultSiteActivity)
	err := json.Unmarshal(message.Bytes(), a)
	return a, err
}

type defaultSiteActivity struct {
	ActivityData string `json:"data"`
	ActivityType string `json:"activity_type" binding:"required"`
	createdAt    time.Time
}

func (a defaultSiteActivity) Data() string {
	return a.ActivityData
}

func (a defaultSiteActivity) Type() string {
	return a.ActivityType
}

func (a defaultSiteActivity) CreatedAt() string {
	return a.createdAt.String()
}
