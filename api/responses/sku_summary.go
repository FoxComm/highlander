package responses

import "github.com/FoxComm/middlewarehouse/models"

type skuCounts struct {
	SKU         string `json:"sku"`
	OnHand      int    `json:"onHand"`
	OnHold      int    `json:"onHold"`
	Reserved    int    `json:"reserved"`
	SafetyStock int    `json:"safetyStock"`
	AFS         int    `json:"afs"`
	AFSCost     int    `json:"afsCost"`
}

type SKUSummary struct {
	Warehouse Warehouse `json:"warehouse"`
	Counts    skuCounts `json:"counts"`
}

func NewSKUSummaryFromModel(sku string, summary *models.StockItemSummary) *SKUSummary {
	return &SKUSummary{
		Warehouse{
			ID:   1,
			Name: "mocked",
		},
		skuCounts{
			SKU:         sku,
			OnHand:      summary.OnHand,
			OnHold:      summary.OnHold,
			Reserved:    summary.Reserved,
			SafetyStock: 0,
			AFS:         summary.OnHand - summary.OnHold - summary.Reserved,
			AFSCost:     0,
		},
	}
}
