package main

import "log"

func main() {
	port := ":9090"
	url := "http://10.240.0.28:8079"

	proxy, err := NewRouterProxy(url)
	if err != nil {
		log.Fatal(err)
	}

	proxy.Run(port)
}
