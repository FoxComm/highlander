package main

import (
	"encoding/json"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/models/activities"
)

type FullOrder struct {
	Order payloads.Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity activities.ISiteActivity) (*FullOrder, error) {
	bt := []byte(activity.Data())
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, err
}
