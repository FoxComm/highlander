package main

import (
	"encoding/json"

	"github.com/FoxComm/middlewarehouse/api/payloads"
)

type FullOrder struct {
	Order payloads.Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity *Activity) (*FullOrder, error) {
	bt := []byte(activity.Data)
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, err
}
