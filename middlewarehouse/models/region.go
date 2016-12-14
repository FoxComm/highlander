package models

type Region struct {
	ID        uint
	Name      string
	CountryID uint
	Country   Country
}
