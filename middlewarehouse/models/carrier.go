package models

type Carrier struct {
	ID               uint
	Name             string
	TrackingTemplate string
	Scope            string
}

func (carrier *Carrier) Identifier() uint {
	return carrier.ID
}
