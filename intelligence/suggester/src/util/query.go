package util

import (
	"encoding/json"
	"errors"
	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
	"net/http"
	"os"
)

var url = os.Getenv("API_URL")
var antHillSrvHost = os.Getenv("ANTHILL_HOST")

func getPort(srvName string) (string, error) {
	var port string
	var portErr error
	_, port, portErr = LookupSrv(srvName)
	if portErr != nil {
		return "", portErr
	}

	return port, nil
}

func AntHillQuery() (responses.AntHillResponse, error) {
	_, err := getPort(antHillSrvHost)
	if err != nil {
		return responses.AntHillResponse{}, errors.New("Unable to locate AntHill Srv Host")
	}

	testAction := "/api/v1/public/recommend/prod-prod/full/5?channel=5"
	resp, reqErr := http.Get(url + testAction)
	if reqErr != nil {
		return responses.AntHillResponse{}, reqErr
	}

	var antHillResponse responses.AntHillResponse
	jsonErr := json.NewDecoder(resp.Body).Decode(&antHillResponse)
	if jsonErr != nil {
		return responses.AntHillResponse{}, jsonErr
	}

	return antHillResponse, nil
}
