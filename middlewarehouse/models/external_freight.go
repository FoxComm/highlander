package models

import "github.com/FoxComm/highlander/middlewarehouse/common/gormfox"

type ExternalFreight struct {
	gormfox.Base
	CarrierID   uint
	MethodName  string
	ServiceCode string
}
