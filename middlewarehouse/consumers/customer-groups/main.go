package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/agent"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/consumer"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/metamorphosis"
)

func main() {

	consumerConfig, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Panicf("Unable to parse consumer config with error %s", err.Error())
	}

	agentConfig, err := makeAgentConfig()
	if err != nil {
		log.Panicf("Unable to initialize agent with error: %s", err.Error())
	}

	phoenixConfig, err := shared.MakePhoenixConfig()
	if err != nil {
		log.Panicf("Unable to initialize agent with error: %s", err.Error())
	}

	// Initialize and start polling agent
	groupsAgent, err := agent.NewAgent(
		agent.SetElasticURL(agentConfig.ElasticURL),
		agent.SetPhoenixURL(phoenixConfig.URL),
		agent.SetPhoenixAuth(phoenixConfig.User, phoenixConfig.Password),
		agent.SetTimeout(agentConfig.PollingInterval),
	)

	if err != nil {
		log.Panicf("Couldn't create ES client with error %s", err.Error())
	}

	groupsAgent.Run()

	// Initialize and start consumer
	cgc, err := consumer.NewCustomerGroupsConsumer(
		consumer.SetPhoenixURL(phoenixConfig.URL),
		consumer.SetPhoenixAuth(phoenixConfig.User, phoenixConfig.Password),
	)

	if err != nil {
		log.Panicf("Couldn't create CGs consumer with error %s", err.Error())
	}

	c, err := metamorphosis.NewConsumer(consumerConfig.ZookeeperURL, consumerConfig.SchemaRepositoryURL, consumerConfig.OffsetResetStrategy)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	c.RunTopic(consumerConfig.Topic, cgc.Handler)
}
