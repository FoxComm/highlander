package payloads

// InsuranceOptions represent how a package can be insured.
type InsuranceOptions struct {
	Provider       string
	InsureShipment bool
	InsuredValue   float64
}
