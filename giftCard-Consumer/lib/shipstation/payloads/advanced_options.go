package payloads

type AdvancedOptions struct {
	WarehouseID       *int `json:"warehouseId"`
	NonMachinable     bool
	SaturdayDelivery  bool
	ContainsAlcohol   bool
	StoreID           *int `json:"storeId"`
	CustomField1      string
	CustomField2      string
	CustomField3      string
	Source            *string
	BillToParty       string
	BillToAccount     string
	BillToPostalCode  string
	BillToCountryCode string
}
