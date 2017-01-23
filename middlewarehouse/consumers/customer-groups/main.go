package main

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/agent"
	"os"
)

const (
	ElasticURLKey      = "ES_URL"
	ElasticIndexKey    = "ES_INDEX"
	PhoenixURLKey      = "PHOENIX_URL"
	PhoenixUserKey     = "PHOENIX_USER"
	PhoenixPasswordKey = "PHOENIX_PASSWORD"
)

func main() {
	esURL := os.Getenv(ElasticURLKey)
	esIndex := os.Getenv(ElasticIndexKey)
	phoenixUrl := os.Getenv(PhoenixURLKey)
	phoenixUser := os.Getenv(PhoenixUserKey)
	phoenixPassword := os.Getenv(PhoenixPasswordKey)

	log.Printf("ES URL: %s", esURL)
	log.Printf("ES Index: %s", esIndex)
	log.Printf("Phoenix URL: %s", phoenixUrl)
	log.Printf("Phoenix Auth: %s:%s", phoenixUser, phoenixPassword)

	groupsAgent, err := agent.NewAgent(
		agent.SetElasticURL(esURL),
		agent.SetElasticIndex(esIndex),
		agent.SetElasticQierySize(3),
		agent.SetPhoenixURL(phoenixUrl),
		agent.SetPhoenixAuth(phoenixUser, phoenixPassword),
		agent.SetTimeout(30*time.Second),
	)

	if err != nil {
		log.Fatalf("Couldn't create ES client with error %s", err)
	}

	groupsAgent.Run()
}
