package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type stockItemSummary struct {
	StockItemID uint   `json:"id"`
	SKU         string `json:"sku"`
	OnHand      int    `json:"onHand"`
	OnHold      int    `json:"onHold"`
	Reserved    int    `json:"reserved"`
	SafetyStock int    `json:"safetyStock"`
	AFS         int    `json:"afs"`
	AFSCost     int    `json:"afsCost"`
}

type StockItemSummary struct {
	Warehouse Warehouse        `json:"warehouse"`
	Counts    stockItemSummary `json:"counts"`
}

type StockItemsSummary struct {
	Warehouse Warehouse          `json:"warehouse"`
	Counts    []stockItemSummary `json:"counts"`
}

func StockItemsSummaryFromModel(summaries []*models.StockItemSummary) *StockItemsSummary {
	result := StockItemsSummary{
		Warehouse: Warehouse{
			ID:   1,
			Name: "mocked",
		},
		Counts: make([]stockItemSummary, len(summaries)),
	}

	for i, summary := range summaries {
		result.Counts[i] = stockItemSummaryFromModel(summary)
	}

	return &result
}

func StockItemSummaryFromModel(summary *models.StockItemSummary) *StockItemSummary {
	return &StockItemSummary{
		Warehouse: Warehouse{
			ID:   1,
			Name: "mocked",
		},
		Counts: stockItemSummaryFromModel(summary),
	}
}

func stockItemSummaryFromModel(summary *models.StockItemSummary) stockItemSummary {
	return stockItemSummary{
		StockItemID: summary.StockItemID,
		SKU:         summary.SKU,
		OnHand:      summary.OnHand,
		OnHold:      summary.OnHold,
		Reserved:    summary.Reserved,
		SafetyStock: 0,
		AFS:         summary.OnHand - summary.OnHold - summary.Reserved,
		AFSCost:     0,
	}
}
