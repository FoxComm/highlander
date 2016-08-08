package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
)

type Region struct {
	ID        uint
	Name      string
	Country   Country
	CountryID uint
}

func NewRegionFromPayload(payload *payloads.Region) *Region {
	return &Region{
		ID:   payload.ID,
		Name: payload.Name,
		Country: Country{
			ID:   payload.CountryID,
			Name: payload.CountryName,
		},
	}
}
