package util

import (
	"errors"
	"net"
	"strconv"
)

func LookupSrv(host string) (string, string, error) {
	_, srvs, err := net.LookupSRV("", "", host)
	if err != nil {
		return host, "", err
	}

	if len(srvs) == 0 {
		return host, "", errors.New("Unable to find port for " + host)
	}

	srv := srvs[0]

	host = srv.Target
	port := strconv.Itoa(int(srv.Port))

	return host, port, nil
}
