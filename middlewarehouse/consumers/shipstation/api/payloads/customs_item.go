package payloads

type CustomsItem struct {
	Description          string
	Quantity             int
	Value                float64
	HarmonizedTariffCode string
	CountryOfOrigin      string
}
