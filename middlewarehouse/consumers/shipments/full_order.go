package main

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/shared/golang/activities"
)

type FullOrder struct {
	Order payloads.Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity activities.SiteActivity) (*FullOrder, error) {
	bt := []byte(activity.Data())
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, err
}
