package payloads

import (
	"fmt"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/phoenix"
)

// Address is a payload for creating and updating addresses.
type Address struct {
	Name        string
	Company     *string
	Street1     string
	Street2     *string
	Street3     *string
	City        string
	State       string
	PostalCode  string
	Country     string
	Phone       *string
	Residential bool
}

func NewAddressFromPhoenix(name string, address phoenix.Address) (*Address, exceptions.IException) {
	state, err := convertRegionFromPhoenix(address.Region.Name)
	if err != nil {
		return nil, err
	}

	if address.Region.CountryID != 234 {
		err := fmt.Errorf("Attempted to create address with country ID = %d. Only the US (234) is supported", address.Region.CountryID)
		return nil, exceptions.NewNotImplementedException(err)
	}

	return &Address{
		Name:        name,
		Street1:     address.Address1,
		Street2:     address.Address2,
		City:        address.City,
		State:       state,
		PostalCode:  address.Zip,
		Country:     "US",
		Residential: true,
	}, nil
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

var countryMap = map[string]string{
	"united states": "US",
}

func convertRegionFromPhoenix(region string) (string, exceptions.IException) {
	key := strings.ToLower(region)
	ssRegion, ok := regionMap[key]
	if !ok {
		return "", exceptions.NewValidationException(fmt.Errorf("Abbreviation not found for %s", region))
	}
	return ssRegion, nil
}
