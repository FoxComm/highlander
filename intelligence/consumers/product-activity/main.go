package main

import (
	"gopkg.in/olivere/elastic.v3"
	"log"
	"os"
	"strconv"
)

func main() {
	var henhouseHost, hostDefined = os.LookupEnv("HENHOUSE_HOST")
	if !hostDefined {
		log.Fatal("HENHOUSE_HOST environment variable is required")
		return
	}

	var henhousePort = os.Getenv("HENHOUSE_HTTP_PORT")
	var elasticHost = os.Getenv("ELASTIC_HOST")
	var interval, intervalDefined = os.LookupEnv("INTERVAL")
	if !intervalDefined {
		interval = "60"
	}

	var esIndex, esIndexDefined = os.LookupEnv("ES_INDEX")

	if !esIndexDefined {
		log.Fatal("ES_INDEX environment variable is required")
		return
	}

	var graphitePort, graphitePortDefined = os.LookupEnv("HENHOUSE_INPUT_PORT")

	if !graphitePortDefined {
		graphitePort = "2003"
	}

	log.Printf("HENHOUSE_HOST: %s\nHENHOUSE_HTTP_PORT: %s\nHENHOUSE_GRAPHITE_PORT: %s\nINTERVAL: %s\nELASTIC_HOST: %s", henhouseHost, henhousePort, graphitePort, interval, elasticHost)

	esClient, err := elastic.NewClient(elastic.SetURL(elasticHost))
	if err != nil {
		log.Panicf("Unable to create ES client with error %s", err.Error())
		return
	}

	httpPort, err := strconv.Atoi(henhousePort)
	if err != nil {
		log.Fatalf("Invalid `HENHOUSE_HTTP_PORT` value %s: %s", henhousePort, err.Error())
		return
	}

	seconds, err := strconv.Atoi(interval)
	if err != nil {
		log.Fatalf("Invalid interval value %s: %s", interval, err.Error())
		return
	}

	inputPort, err := strconv.Atoi(graphitePort)
	if err != nil {
		log.Fatalf("Invalid input port: %s", graphitePort)
	}

	hh, err := NewHenhouse(henhouseHost, int16(httpPort), int16(inputPort))

	if err != nil {
		log.Printf("Error connecting henhouse: %s", err)
		return
	}

	oc, err := NewProductActivityMonitor(hh, esClient, esIndex, seconds)
	if err != nil {
		log.Printf("Error creating consumer: %s", err)
		return
	}

	oc.start()
}
