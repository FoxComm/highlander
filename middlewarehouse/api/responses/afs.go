package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type AFS struct {
	StockItemID uint   `json:"stockItemId"`
	SkuID       uint   `json:"skuId"`
	SkuCode     string `json:"skuCode"`
	AFS         int    `json:"afs"`
}

func NewAFSFromModel(afs *models.AFS) *AFS {
	return &AFS{
		afs.StockItemID,
		afs.SkuID,
		afs.SkuCode,
		afs.AFS,
	}
}
