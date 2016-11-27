package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/metamorphosis"
)

// SiteActivity is as action that occurs within the system that should be
// written to the activity log.
type ISiteActivity interface {
	Id() int
	Type() string
	Data() string
	CreatedAt() string
}

func NewActivityFromAvro(message metamorphosis.AvroMessage) (ISiteActivity, exceptions.IException) {
	a := new(defaultSiteActivity)
	err := json.Unmarshal(message.Bytes(), a)
	return a, NewActivityException(err)
}

type defaultSiteActivity struct {
	ActivityId   int    `json:"id"`
	ActivityData string `json:"data"`
	ActivityType string `json:"activity_type" binding:"required"`
	createdAt    time.Time
}

func (a defaultSiteActivity) Id() int {
	return a.ActivityId
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
