package gormfox

import "github.com/FoxComm/middlewarehouse/common/validation"

type Model interface {
	Identifier() uint
	Validate(repository Repository) ([]validation.Invalid, error)
}
