package utils

import (
	"errors"
	"math/rand"
	"net"
	"strconv"

	_ "github.com/lib/pq"
)

func LookupHostAndPort(host string) (string, string, error) {
	//Lookup SRV record for host
	_, srvs, err := net.LookupSRV("", "", host)
	if err != nil {
		return host, "", err
	}

	//Select Random SRV record
	sz := len(srvs)

	if sz == 0 {
		return host, "", errors.New("Unable to find port for " + host)
	}

	selected := rand.Intn(sz)
	srv := srvs[selected]

	//We return host here too so that request goes to correct machine.
	host = srv.Target
	port := strconv.Itoa(int(srv.Port))

	return host, port, nil
}
