package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type Reservation struct {
	gormfox.Base
	RefNum string
}

func (r Reservation) Identifier() uint {
	return r.ID
}

func MakeReservationFromPayload(payload payloads.Reservation) Reservation {
	return Reservation{RefNum: payload.RefNum}
}
