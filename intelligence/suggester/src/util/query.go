package util

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
)

var url = os.Getenv("API_URL")
var antHillSrvHost = os.Getenv("ANTHILL_HOST")

func PingAntHill() {
	antHillPort, err := getPort(antHillSrvHost)
	if err != nil {
		return nil, err
	}

	resp, reqErr := http.Get(url + ":" + antHillPort + "/ping")
	if reqErr != nil {
		return nil, reqErr
	}

	fmt.Println(resp)
}

func getPort(srvName string) (string, error) {
	if port == "" {
		var portErr error
		_, port, portErr = LookupSrv(srvName)
		if portErr != nil {
			return "", portErr
		}
	}

	return port, nil
}
