package ups

import "strconv"

// RateResponse is the response from the UPS API when trying to get the rate for a shipment.
type RateResponse struct {
	RateResponse rateResponseInner `json:"RateResponse"`
}

func (r *RateResponse) IsSuccess() bool {
	return r.RateResponse.Response.ResponseStatus.Code == "1"
}

func (r *RateResponse) TotalCharges() (float64, error) {
	charge := r.RateResponse.RatedShipment.TotalCharges.MonetaryValue
	return strconv.ParseFloat(charge, 64)
}

type rateResponseInner struct {
	Response      response      `json:"Response"`
	RatedShipment ratedShipment `json:"RatedShipment"`
}

type response struct {
	ResponseStatus responseStatus `json:"ResponseStatus"`
}

type ratedShipment struct {
	TotalCharges charges `json:"TotalCharges"`
}

type responseStatus struct {
	Code        string `json:"Code"`
	Description string `json:"Description"`
}

type charges struct {
	CurrencyCode  string `json:"CurrencyCode"`
	MonetaryValue string `json:"MonetaryValue"`
}
