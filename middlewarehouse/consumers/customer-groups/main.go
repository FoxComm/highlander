package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/agent"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/consumer"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/mailchimp"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
	"gopkg.in/olivere/elastic.v3"
	//"os"
)

func main() {
	consumerConfig, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Panicf("Unable to parse consumer config with error %s", err.Error())
	}

	agentConfig, err := makeAgentConfig()
	if err != nil {
		log.Panicf("Unable to parse agent config with error: %s", err.Error())
	}

	phoenixConfig, err := shared.MakePhoenixConfig()
	if err != nil {
		log.Panicf("Unable to parse phoenix config with error: %s", err.Error())
	}

	// new ES client
	esClient, err := elastic.NewClient(
		elastic.SetURL(agentConfig.ElasticURL),
		//elastic.SetTraceLog(log.New(os.Stdout, "", 0)),
	)
	if err != nil {
		log.Panicf("Unable to create ES client with error %s", err.Error())
	}

	// new Phoenix client
	phoenixClient := phoenix.NewPhoenixClient(phoenixConfig.URL, phoenixConfig.User, phoenixConfig.Password)
	//
	log.Println(agentConfig.MailchimpAPIKey)
	chimpClient := mailchimp.NewClient(agentConfig.MailchimpAPIKey, mailchimp.SetDebug(true))

	//Initialize and start polling agent
	groupsAgent, err := agent.NewAgent(
		esClient,
		phoenixClient,
		chimpClient,
		agent.SetMailchimpListID(agentConfig.MailchimpListId),
		agent.SetTimeout(agentConfig.PollingInterval),
	)

	if err != nil {
		log.Panicf("Unable to initialize CGs agent with error %s", err.Error())
	}

	groupsAgent.Run()

	// Initialize and start consumer
	cgc, err := consumer.NewCustomerGroupsConsumer(
		esClient,
		phoenixClient,
		chimpClient,
		consumer.SetMailchimpListID(agentConfig.MailchimpListId),
	)

	if err != nil {
		log.Panicf("Unable ti initialize CGs consumer with error %s", err.Error())
	}

	c, err := metamorphosis.NewConsumer(consumerConfig.ZookeeperURL, consumerConfig.SchemaRepositoryURL, consumerConfig.OffsetResetStrategy)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	c.RunTopic(consumerConfig.Topic, cgc.Handler)
}
