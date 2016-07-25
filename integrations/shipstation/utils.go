package main

import (
	"fmt"
	"strings"
)

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

func convertRegionFromPhoenix(region string) (string, error) {
	key := strings.ToLower(region)
	ssRegion, ok := regionMap[key]
	if !ok {
		return "", fmt.Errorf("Abbreviation not found for %s", region)
	}
	return ssRegion, nil
}

func convertCountryFromPhoenix(country string) (string, error) {
	key := strings.ToLower(country)
	ssCountry, ok := countryMap[key]
	if !ok {
		return "", fmt.Errorf("Abbreviation not found for %s", country)
	}
	return ssCountry, nil
}
