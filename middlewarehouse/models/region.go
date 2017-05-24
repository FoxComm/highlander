package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
)

type Region struct {
	ID        uint
	Name      string
	CountryID uint
	Country   Country
}

func NewRegionFromPayload(payload *payloads.Region) *Region {
	return &Region{
		ID:        payload.ID,
		Name:      payload.Name,
		CountryID: payload.CountryID,
		Country: Country{
			ID:   payload.CountryID,
			Name: payload.CountryName,
		},
	}
}
