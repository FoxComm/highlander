package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type AFS struct {
	StockItemID uint   `json:"stockItemId"`
	SKU         string `json:"sku"`
	AFS         int    `json:"afs"`
	Scope       string `json:"scope"`
}

func NewAFSFromModel(afs *models.AFS) *AFS {
	return &AFS{
		afs.StockItemID,
		afs.SKU,
		afs.AFS,
        afs.Scope,
	}
}
