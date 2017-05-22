package main

import "log"

func main() {
	host := "http://localhost:9090"
	origin := "http://10.240.0.28:8079"

	proxy, err := NewRouterProxy(host, origin)
	if err != nil {
		log.Fatal(err)
	}

	proxy.Run()
}
