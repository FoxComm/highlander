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

	//segments, err := chimpClient.GetSegments(agentConfig.MailchimpListId)
	//if err != nil {
	//	log.Panic(err.Error())
	//}
	//
	//log.Printf("Segments count: %d", segments.Total)
	//
	//s1 := rand.NewSource(time.Now().UnixNano())
	//id := rand.New(s1).Intn(100)
	//
	//newName := fmt.Sprintf("%d#Test Segment", id)
	//segmentAlreadyExists := false
	//
	//for _, s := range segments.Segments {
	//	log.Printf("Segment type: %s, name: %s", s.Type, s.Name)
	//
	//	if s.Name == newName {
	//		segmentAlreadyExists = true
	//	} else {
	//		if _, err := chimpClient.UpdateStaticSegment(agentConfig.MailchimpListId, s.ID, &mailchimp.SegmentPayload{
	//			Name:          s.Name + " Updated",
	//			StaticSegment: []string{},
	//		}); err != nil {
	//			log.Panic(err.Error())
	//		}
	//	}
	//
	//	//if err := chimpClient.DeleteStaticSegment(agentConfig.MailchimpListId, s.ID); err != nil {
	//	//	log.Panic(err.Error())
	//	//}
	//}

	//if !segmentAlreadyExists {
	//l := rand.New(s1).Intn(10)
	//
	//emails := []string{}
	//for i := 0; i < l; i++ {
	//	id := rand.New(s1).Intn(4) + 1
	//	emails = append(emails, fmt.Sprintf("tony+%d@foxcommerce.com", id))
	//}
	//
	//segment, err := chimpClient.CreateSegment(agentConfig.MailchimpListId, &mailchimp.SegmentPayload{
	//	Name:          newName,
	//	StaticSegment: emails,
	//})
	//
	//if err != nil {
	//	log.Panicf(err.Error())
	//}
	//
	//log.Printf("New Segment name: %s", segment.Name)
	////}
	//
	//time.Sleep(15 * time.Second)
	//
	//if _, err := chimpClient.UpdateStaticSegment(agentConfig.MailchimpListId, segment.ID, &mailchimp.SegmentPayload{
	//	Name:          newName + " Updated 1",
	//	StaticSegment: []string{"tony@foxcommerce.com", "tony+1@foxcommerce.com", "tony+2@foxcommerce.com", "tony+3@foxcommerce.com", "tony+4@foxcommerce.com"},
	//}); err != nil {
	//	log.Panic(err.Error())
	//}
	//
	//time.Sleep(15 * time.Second)
	//
	//if _, err := chimpClient.UpdateStaticSegment(agentConfig.MailchimpListId, segment.ID, &mailchimp.SegmentPayload{
	//	Name:          newName + " Updated 2",
	//	StaticSegment: []string{"tony+1@foxcommerce.com", "tony+4@foxcommerce.com"},
	//}); err != nil {
	//	log.Panic(err.Error())
	//}
	//
	//time.Sleep(15 * time.Second)
	//
	//if _, err := chimpClient.UpdateStaticSegment(agentConfig.MailchimpListId, segment.ID, &mailchimp.SegmentPayload{
	//	Name:          newName + " Updated 3",
	//	StaticSegment: []string{"tony+4@foxcommerce.com"},
	//}); err != nil {
	//	log.Panic(err.Error())
	//}

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
