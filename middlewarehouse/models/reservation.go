package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
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
