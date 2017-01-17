package util

import (
	"encoding/json"
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/responses"
	"net/http"
	"os"
)

var url = os.Getenv("API_URL")
var port = os.Getenv("HENHOUSE_PORT")

func ProductQuery(id string, steps []string, a, b string) (responses.HenhouseResponse, error) {
	key := ""
	for _, step := range steps {
		key += "track_product_" + id + "_" + step + ","
	}

	if a != "" {
		key += "&a=" + a
	}
	if b != "" {
		key += "&b=" + b
	}

	port, err := getPort()
	if err != nil {
		return nil, err
	}

	resp, reqErr := http.Get(url + ":" + port + "/diff?keys=" + key)
	if reqErr != nil {
		return nil, reqErr
	}

	var pf responses.HenhouseResponse
	jsonErr := json.NewDecoder(resp.Body).Decode(&pf)
	if jsonErr != nil {
		return nil, jsonErr
	}
	return pf, nil
}

func getPort() (string, error) {
	if port == "" {
		var portErr error
		_, port, portErr = LookupSrv("henhouse.service.consul")
		if portErr != nil {
			return "", portErr
		}
	}
	return port, nil
}
