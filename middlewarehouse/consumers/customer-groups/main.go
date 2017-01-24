package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/agent"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
)

func main() {
	agentConfig, err := makeAgentConfig()
	if err != nil {
		log.Panicf("Unable to initialize agent with error: %s", err.Error())
	}

	phoenixConfig, err := shared.MakePhoenixConfig()
	if err != nil {
		log.Panicf("Unable to initialize agent with error: %s", err.Error())
	}

	groupsAgent, err := agent.NewAgent(
		agent.SetElasticURL(agentConfig.ElasticURL),
		agent.SetPhoenixURL(phoenixConfig.URL),
		agent.SetPhoenixAuth(phoenixConfig.User, phoenixConfig.Password),
		agent.SetTimeout(agentConfig.PollingInterval),
	)

	if err != nil {
		log.Panicf("Couldn't create ES client with error %s", err)
	}

	groupsAgent.Run()
}
