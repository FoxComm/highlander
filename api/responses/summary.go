package responses

import "github.com/FoxComm/middlewarehouse/models"

type stockItemSummary struct {
	StockItemID       uint   `json:"stockItemId"`
	SKU               string `json:"sku"`
	StockLocationID   uint   `json:"stockLocationId"`
	StockLocationName string `json:"stockLocationName"`
	Type              string `json:"type"`
	OnHand            int    `json:"onHand"`
	OnHold            int    `json:"onHold"`
	Reserved          int    `json:"reserved"`
	Shipped           int    `json:"shipped"`
	AFS               int    `json:"afs"`
	AFSCost           int    `json:"afsCost"`
}

type Summary struct {
	Summary []stockItemSummary `json:"summary"`
}

func NewSummaryFromModel(summaries []*models.StockItemSummary) *Summary {
	result := Summary{
		Summary: make([]stockItemSummary, len(summaries)),
	}

	for i, summary := range summaries {
		result.Summary[i] = stockItemSummaryFromModel(summary)
	}

	return &result
}

func stockItemSummaryFromModel(summary *models.StockItemSummary) stockItemSummary {
	return stockItemSummary{
		StockItemID:       summary.StockItemID,
		SKU:               summary.SKU,
		StockLocationID:   summary.StockLocationID,
		StockLocationName: summary.StockLocationName,
		Type:              models.StockItemTypeToString(summary.TypeID),
		OnHand:            summary.OnHand,
		OnHold:            summary.OnHold,
		Reserved:          summary.Reserved,
		Shipped:           summary.Shipped,
		AFS:               summary.AFS,
		AFSCost:           summary.AFSCost,
	}
}
