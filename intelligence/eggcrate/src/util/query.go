package util

import (
	"encoding/json"
	"errors"
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/responses"
	"net/http"
	"os"
)

var url = os.Getenv("API_URL")
var port = os.Getenv("HENHOUSE_PORT")

func HenhouseQuery(action string, keys []string, from, to string, params string) (responses.HenhouseResponse, error) {
	key := ""

	if action != "diff" && action != "summary" {
		return nil, errors.New("The action " + action + " is not supported")
	}

	for _, k := range keys {
		key += k + ","
	}

	if from != "" {
		key += "&a=" + from
	}

	if to != "" {
		key += "&b=" + to
	}

	if params != "" {
		key += "&" + params
	}

	if params != "" {
		key += "&" + params
	}

	port, err := getPort()
	if err != nil {
		return nil, err
	}

	resp, reqErr := http.Get(url + ":" + port + "/" + action + "?keys=" + key)
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
