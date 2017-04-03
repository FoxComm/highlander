package main

import (
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/util"
	"gopkg.in/olivere/elastic.v3"
	"log"
	"os"
	"strconv"
)

func main() {

	var henhouseLookup = lookupEnvOr("HENHOUSE", "henhouse.service.consul")
	host, port, err := util.LookupSrv(henhouseLookup)
	if err != nil {
		log.Printf("No address found for: %s", henhouseLookup)
	}
	var henhouseHost = lookupEnvOr("HENHOUSE_HOST", host)
	port = lookupEnvOr("HENHOUSE_HTTP_PORT", port)

	henhouseHttpPort, err := strconv.Atoi(port)
	if err != nil {
		log.Fatalf("Invalid port for henhouse.service.consul:%s", port)
		return
	}

	var elasticUrl = os.Getenv("ELASTIC_URL")
	var interval, intervalDefined = os.LookupEnv("INTERVAL")
	if !intervalDefined {
		interval = "60"
	}

	var esIndex, esIndexDefined = os.LookupEnv("ELASTIC_INDEX")
	if !esIndexDefined {
		log.Fatal("ES_INDEX environment variable is required")
		return
	}

	var graphitePort, graphitePortDefined = os.LookupEnv("HENHOUSE_INPUT_PORT")
	if !graphitePortDefined {
		graphitePort = "2003"
	}

	log.Printf("HENHOUSE_HOST: %s\nHENHOUSE_HTTP_PORT: %d\nHENHOUSE_INPUT_PORT: %s\nINTERVAL: %s\nELASTIC_URL: %s\nELASTIC_INDEX: %s", henhouseHost, henhouseHttpPort, graphitePort, interval, elasticUrl, esIndex)

	esClient, err := elastic.NewClient(elastic.SetURL(elasticUrl))
	if err != nil {
		log.Panicf("Unable to create ES client with error %s", err.Error())
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

	hh, err := NewHenhouse(henhouseHost, int16(henhouseHttpPort), int16(inputPort))
	if err != nil {
		log.Printf("Error connecting henhouse: %s", err)
		return
	}

	daemon, err := NewProductActivityDaemon(hh, esClient, esIndex, seconds)
	if err != nil {
		log.Printf("Error creating consumer: %s", err)
		return
	}

	daemon.start()
}

func lookupEnvOr(varible string, defaultValue string) string {
	value, exists := os.LookupEnv(varible)
	if !exists {
		return defaultValue
	} else {
		return value
	}
}
