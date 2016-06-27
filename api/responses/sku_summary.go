package responses

type skuCounts struct {
	OnHand      int `json:"onHand"`
	OnHold      int `json:"onHold"`
	Reserved    int `json:"reserved"`
	SafetyStock int `json:"safetyStock"`
	AFS         int `json:"afs"`
	AFSCost     int `json:"afsCost"`
}

type SKUSummary struct {
	Warehouse Warehouse `json:"warehouse"`
	Counts    skuCounts `json:"counts"`
}

func NewSKUSummary() *SKUSummary {
	// TODO: This is just a mock, make the values real at some point in the future.
	sc := skuCounts{
		OnHand:      10639,
		OnHold:      663,
		Reserved:    52,
		SafetyStock: 1,
		AFS:         9923,
		AFSCost:     34730500,
	}

	wh := Warehouse{
		ID:   1,
		Name: "default",
	}

	return &SKUSummary{Warehouse: wh, Counts: sc}
}
