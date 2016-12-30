package ups

import (
	"encoding/json"
	"errors"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

const (
	testingURL    = "https://wwwcie.ups.com/rest/Rate"
	productionURL = "https://onlinetools.ups.com/rest/Rate"
)

// API implements the interface to get interact with the UPS API.
type API struct {
	username    string
	password    string
	accessToken string
	url         string
}

// NewAPI creates a new API object.
func NewAPI(username, password, accessToken string, production bool) *API {
	var url string
	if production {
		url = productionURL
	} else {
		url = testingURL
	}

	return &API{username, password, accessToken, url}
}

func (u *API) GetRate(shipment *models.Shipment) (float64, error) {
	auth := NewAuthPayload(u.username, u.password, u.accessToken)

	if len(shipment.ShipmentLineItems) == 0 {
		return 0.0, errors.New("Shipment must have at least one line item to calculate the rate")
	}

	// Get the shipping location from the address of the first stock location.
	// Since all line items in a shipment are from the same stock location, getting
	// the first one is sufficient.
	stockLocation := shipment.ShipmentLineItems[0].StockItemUnit.StockItem.StockLocation
	shipper := stockLocation.Address
	shipFrom := stockLocation.Address

	shipTo := shipment.Address

	// TODO: Don't hardcode this as UPS Ground.
	freight := &models.ExternalFreight{
		MethodName:  "UPS Ground",
		ServiceCode: "03",
	}

	// TODO: Get the weight from the SKU, once that's in MWH.
	weightUnits := "lbs"
	weight := "1"

	ratePayload, err := NewRatePayload(auth, &shipper, &shipFrom, &shipTo, freight, weightUnits, weight)
	if err != nil {
		return 0.0, err
	}

	headers := map[string]string{}
	resp, err := consumers.Post(u.url, headers, ratePayload)
	if err != nil {
		return 0.0, err
	}

	defer resp.Body.Close()
	rateResp := new(RateResponse)
	if err := json.NewDecoder(resp.Body).Decode(rateResp); err != nil {
		return 0.0, err
	}

	if !rateResp.IsSuccess() {
		return 0.0, errors.New("Error requesting rate")
	}

	return rateResp.TotalCharges()
}
