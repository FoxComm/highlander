package responses

import (
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type stockItemSummary struct {
	SKU           string         `json:"sku"`
	StockItem     *StockItem     `json:"stockItem"`
	StockLocation *StockLocation `json:"stockLocation"`
	Type          string         `json:"type"`
	OnHand        int            `json:"onHand"`
	OnHold        int            `json:"onHold"`
	Reserved      int            `json:"reserved"`
	Shipped       int            `json:"shipped"`
	AFS           int            `json:"afs"`
	AFSCost       int            `json:"afsCost"`
	CreatedAt     time.Time      `json:"createdAt"`
}

type StockItemSummary struct {
	Summary []stockItemSummary `json:"summary"`
}

func NewSummaryFromModel(summaries []*models.StockItemSummary) *StockItemSummary {
	result := StockItemSummary{
		Summary: make([]stockItemSummary, len(summaries)),
	}

	for i, summary := range summaries {
		result.Summary[i] = summaryFromModel(summary)
	}

	return &result
}

func summaryFromModel(summary *models.StockItemSummary) stockItemSummary {
	return stockItemSummary{
		SKU:           summary.StockItem.SKU,
		StockItem:     NewStockItemFromModel(&summary.StockItem),
		StockLocation: NewStockLocationFromModel(&summary.StockItem.StockLocation),
		Type:          string(summary.Type),
		OnHand:        summary.OnHand,
		OnHold:        summary.OnHold,
		Reserved:      summary.Reserved,
		Shipped:       summary.Shipped,
		AFS:           summary.AFS,
		AFSCost:       summary.AFSCost,
		CreatedAt:     summary.CreatedAt,
	}
}
