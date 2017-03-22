package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/agent"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/consumer"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/manager"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/mailchimp"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"

	"gopkg.in/olivere/elastic.v3"
)

const MESSAGING_PLUGIN_NAME = "messaging"
const MESSAGING_SETTINGS_KEY_MAILCHIMP_API_KEY = "mailchimp_key"
const MESSAGING_SETTINGS_KEY_MAILCHIMP_LIST_ID = "mailchimp_customers_list_id"

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
	esClient, err := elastic.NewClient(elastic.SetURL(agentConfig.ElasticURL))
	if err != nil {
		log.Panicf("Unable to create ES client with error %s", err.Error())
	}

	// new Phoenix client
	phoenixClient := phoenix.NewPhoenixClient(phoenixConfig.URL, phoenixConfig.User, phoenixConfig.Password)

	mailchimpApiKey, mailchimpListID, err := getMailchimpSettings(phoenixClient)
	if err != nil {
		log.Panicf("Unable to get %s settings with error %s", MESSAGING_PLUGIN_NAME, err.Error())
	}

	mailchimpDisabled := mailchimpApiKey == "" || mailchimpListID == ""
	if mailchimpDisabled {
		log.Printf("Mailchimp config is not complete. For mailchimp integration set up API key and Customers ist ID values in %s plugin settgins",
			MESSAGING_PLUGIN_NAME,
		)
	}

	// new Mailchimp client
	chimpClient := mailchimp.NewClient(mailchimpApiKey, mailchimp.SetDebug(true))

	// Customer Groups manager
	groupsManager := manager.NewGroupsManager(
		esClient,
		phoenixClient,
		chimpClient,
		manager.SetMailchimpListID(mailchimpListID),
		manager.SetMailchimpDisabled(mailchimpDisabled),
	)

	//Initialize and start polling agent
	groupsAgent := agent.NewAgent(
		phoenixClient,
		groupsManager,
		agent.SetTimeout(agentConfig.PollingInterval),
	)

	groupsAgent.Run()

	// Initialize and start consumer
	cgc := consumer.NewCustomerGroupsConsumer(groupsManager)

	c, err := metamorphosis.NewConsumer(consumerConfig.ZookeeperURL, consumerConfig.SchemaRepositoryURL, consumerConfig.OffsetResetStrategy)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	c.RunTopic(consumerConfig.Topic, cgc.Handler)
}

func getMailchimpSettings(phoenixClient phoenix.PhoenixClient) (string, string, error) {
	plugins, err := phoenixClient.GetPlugins()
	if err != nil {
		log.Panicf("Couldn't get plugins list with error %s", err.Error())
	}

	for _, plugin := range plugins {
		if plugin.Name == MESSAGING_PLUGIN_NAME {
			s, err := phoenixClient.GetPluginSettings(plugin.Name)
			if err != nil {
				log.Panicf("Couldn't get %s plugin settings with error %s", MESSAGING_PLUGIN_NAME, err.Error())
			}

			mailchimpApiKey, ok := s[MESSAGING_SETTINGS_KEY_MAILCHIMP_API_KEY].(string)
			if !ok {
				return "", "", fmt.Errorf("%s is not a string. value: %v", MESSAGING_SETTINGS_KEY_MAILCHIMP_API_KEY, mailchimpApiKey)
			}
			mailchimpListID, ok := s[MESSAGING_SETTINGS_KEY_MAILCHIMP_LIST_ID].(string)
			if !ok {
				return "", "", fmt.Errorf("%s is not a string. value: %v", MESSAGING_SETTINGS_KEY_MAILCHIMP_LIST_ID, mailchimpListID)
			}

			return mailchimpApiKey, mailchimpListID, nil
		}
	}

	return "", "", nil
}
