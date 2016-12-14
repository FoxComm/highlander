package models

import "github.com/FoxComm/highlander/middlewarehouse/common/gormfox"

type StockLocation struct {
	gormfox.Base
	Name    string
	Type    string
	Address *Address
	Scope   string
}
