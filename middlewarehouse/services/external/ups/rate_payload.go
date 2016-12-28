package ups

import (
	"fmt"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

// RatePayload is the payload that must be sent to the UPS API to determine the
// rate to ship a package.
type RatePayload struct {
	UPSSecurity AuthPayload `json:"UPSSecurity"`
	RateRequest rateRequest `json:"RateRequest"`
}

// NewRatePayload creates a new RatePayload.
func NewRatePayload(auth *AuthPayload, shipper *models.Address, shipFrom *models.Address,
	shipTo *models.Address, freightMethod *models.ExternalFreight,
	weightUnits string, weightValue string) (*RatePayload, error) {

	shipperLocation := addressLocation{}
	if err := shipperLocation.FromModel(shipper); err != nil {
		return nil, err
	}

	shipFromLocation := addressLocation{}
	if err := shipFromLocation.FromModel(shipFrom); err != nil {
		return nil, err
	}

	shipToLocation := addressLocation{}
	if err := shipToLocation.FromModel(shipTo); err != nil {
		return nil, err
	}

	payload := RatePayload{
		UPSSecurity: *auth,
		RateRequest: rateRequest{
			Request: requestOption{
				RequestOption: "Rate",
			},
			Shipment: shipment{
				Shipper:  shipperLocation,
				ShipTo:   shipToLocation,
				ShipFrom: shipFromLocation,
				Service: code{
					Code:        freightMethod.ServiceCode,
					Description: freightMethod.MethodName,
				},
				Package: upsPackage{
					PackagingType: code{
						Code:        "02",
						Description: "Rate",
					},
					PackageWeight: weight{
						UnitOfMeasurement: code{
							Code: weightUnits,
						},
						Weight: weightValue,
					},
				},
			},
		},
	}

	return &payload, nil
}

type rateRequest struct {
	Request  requestOption `json:"Request"`
	Shipment shipment      `json:"Shipment"`
}

type shipment struct {
	Shipper  addressLocation `json:"Shipper"`
	ShipTo   addressLocation `json:"ShipTo"`
	ShipFrom addressLocation `json:"ShipFrom"`
	Service  code            `json:"Service"`
	Package  upsPackage      `json:"Package"`
}

type address struct {
	AddressLine       []string `json:"AddressLine"`
	City              string   `json:"City"`
	StateProvinceCode string   `json:"StateProvinceCode"`
	PostalCode        string   `json:"PostalCode"`
	CountryCode       string   `json:"CountryCode"`
}

type addressLocation struct {
	Name          string  `json:"Name"`
	ShipperNumber *string `json:"ShipperNumber"`
	Address       address `json:"Address"`
}

func (a *addressLocation) FromModel(model *models.Address) error {
	a.Name = model.Name

	addressLines := []string{model.Address1}
	if model.Address2.Valid {
		addressLines = append(addressLines, model.Address2.String)
	}

	region, ok := regionMap[strings.ToLower(model.Region.Name)]
	if !ok {
		return fmt.Errorf("Unable to get short region for %s", model.Region.Name)
	}

	a.Address = address{
		AddressLine:       addressLines,
		City:              model.City,
		StateProvinceCode: region,
		PostalCode:        model.Zip,
		CountryCode:       "US",
	}

	return nil
}

type code struct {
	Code        string `json:"Code"`
	Description string `json:"Description"`
}

type weight struct {
	UnitOfMeasurement code   `json:"UnitOfMeasurement"`
	Weight            string `json:"Weight"`
}

type upsPackage struct {
	PackagingType code   `json:"PackagingType"`
	PackageWeight weight `json:"PackageWeight"`
}

type requestOption struct {
	RequestOption string `json:"RequestOption"`
}

var regionMap = map[string]string{
	"alabama":              "AL",
	"alaska":               "AK",
	"arizona":              "AR",
	"arkansas":             "AK",
	"california":           "CA",
	"colorado":             "CO",
	"connecticut":          "CT",
	"delaware":             "DE",
	"florida":              "FL",
	"georgia":              "GA",
	"hawaii":               "HI",
	"idaho":                "ID",
	"illinois":             "IL",
	"indiana":              "ID",
	"iowa":                 "IA",
	"kansas":               "KA",
	"kentucky":             "KY",
	"louisiana":            "LA",
	"maine":                "ME",
	"maryland":             "MD",
	"massachusetts":        "MA",
	"michigan":             "MI",
	"minnesota":            "MN",
	"mississippi":          "MS",
	"missouri":             "MO",
	"montana":              "MT",
	"nebraska":             "NE",
	"nevada":               "NV",
	"new hampshire":        "NH",
	"new jersey":           "NJ",
	"new mexico":           "NM",
	"new york":             "NY",
	"north carolina":       "NC",
	"north dakota":         "ND",
	"ohio":                 "OH",
	"oklahoma":             "OK",
	"oregon":               "OR",
	"pennsylvania":         "PA",
	"rhode island":         "RI",
	"south carolina":       "SC",
	"south dakota":         "ND",
	"tennessee":            "TN",
	"texas":                "TX",
	"utah":                 "UT",
	"virginia":             "VA",
	"vermont":              "VT",
	"washington":           "WA",
	"west virginia":        "WV",
	"wisconsin":            "WI",
	"wyoming":              "WY",
	"district of columbia": "DC",
	"american samoa":       "AS",
	"guam":                 "GU",
	"northern mariana islands": "MP",
	"puerto rico":              "PR",
}
