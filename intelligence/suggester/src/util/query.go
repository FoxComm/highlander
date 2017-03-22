package util

import (
	"fmt"
	"net/http"
	"os"
)

var url = os.Getenv("API_URL")
var antHillSrvHost = os.Getenv("ANTHILL_HOST")

func PingAntHill() error {
	antHillPort, err := getPort(antHillSrvHost)
	if err != nil {
		return err
	}

	resp, reqErr := http.Get(url + ":" + antHillPort + "/ping")
	if reqErr != nil {
		return reqErr
	}

	fmt.Println(resp)

	return nil
}

func getPort(srvName string) (string, error) {
	var port string
	var portErr error
	_, port, portErr = LookupSrv(srvName)
	if portErr != nil {
		return "", portErr
	}

	return port, nil
}
