package models

type AFS struct {
	StockItemID uint
	SKU         string
	AFS         int
}

type AfsByType struct {
	Afs  int
	Type UnitType
}
