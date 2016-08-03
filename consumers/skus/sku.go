package main

import (
	"encoding/json"

	"github.com/FoxComm/metamorphosis"
)

type SKU struct {
	ID   int    `json:"id" binding:"required"`
	Code string `json:"code" binding:"required"`
}

func NewSKUFromAvro(message metamorphosis.AvroMessage) (*SKU, error) {
	s := new(SKU)
	if err := json.Unmarshal(message.Bytes(), s); err != nil {
		return nil, err
	}

	return s, nil
}
