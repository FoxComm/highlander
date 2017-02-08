package agent

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/manager"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"

	"github.com/FoxComm/highlander/middlewarehouse/shared/mailchimp"
	"gopkg.in/olivere/elastic.v3"
)

const (
	DefaultElasticIndex    = "admin"
	DefaultElasticTopic    = "customers_search_view"
	DefaultElasticSize     = 100
	DefaultMailchimpListID = ""
	DefaultTimeout         = 30 * time.Minute
)

type Agent struct {
	esClient      *elastic.Client
	phoenixClient phoenix.PhoenixClient
	chimpClient   *mailchimp.ChimpClient
	esTopic       string
	esSize        int
	chimpListID   string
	timeout       time.Duration
}

type AgentOptionFunc func(*Agent)

func SetTimeout(t time.Duration) AgentOptionFunc {
	return func(a *Agent) {
		a.timeout = t
	}
}

func SetElasticTopic(topic string) AgentOptionFunc {
	return func(a *Agent) {
		a.esTopic = topic
	}
}

func SetElasticQierySize(size int) AgentOptionFunc {
	return func(a *Agent) {
		a.esSize = size
	}
}

func SetMailchimpListID(id string) AgentOptionFunc {
	return func(a *Agent) {
		a.chimpListID = id
	}
}

func NewAgent(esClient *elastic.Client,
	phoenixClient phoenix.PhoenixClient,
	chimpClient *mailchimp.ChimpClient,
	options ...AgentOptionFunc) (*Agent, error) {
	agent := &Agent{
		esClient,
		phoenixClient,
		chimpClient,
		DefaultElasticTopic,
		DefaultElasticSize,
		DefaultMailchimpListID,
		DefaultTimeout,
	}

	// set options to agent
	for _, opt := range options {
		opt(agent)
	}

	return agent, nil
}

func (agent *Agent) Run() {
	log.Println("Running customer-groups agent")

	ticker := time.NewTicker(agent.timeout)

	go func() {
		for {
			select {
			case <-ticker.C:
				if err := agent.processGroups(); err != nil {
					log.Panicf("An error occured processing groups: %s", err)
				}
			}
		}
	}()
}

func (agent *Agent) processGroups() error {
	groups, err := agent.phoenixClient.GetCustomerGroups()
	if err != nil {
		return err
	}

	log.Printf("Groups count: %d", len(groups))

	for _, group := range groups {
		manager.ProcessGroup(agent.esClient, agent.phoenixClient, agent.chimpClient, group, agent.esTopic, agent.esSize, agent.chimpListID)
	}

	return nil
}
