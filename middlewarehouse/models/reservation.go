package models

import "github.com/FoxComm/highlander/middlewarehouse/common/gormfox"

type Reservation struct {
	gormfox.Base
	RefNum string
	Scope  string
}

func (r Reservation) Identifier() uint {
	return r.ID
}
