package agent

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/manager"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"

	"gopkg.in/olivere/elastic.v3"
)

const (
	DefaultElasticIndex = "admin"
	DefaultElasticTopic = "customers_search_view"
	DefaultElasticSize  = 100
	DefaultTimeout      = 30 * time.Minute
)

type Agent struct {
	esClient      *elastic.Client
	phoenixClient phoenix.PhoenixClient
	esTopic       string
	esSize        int
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

func NewAgent(esClient *elastic.Client, phoenixClient phoenix.PhoenixClient, options ...AgentOptionFunc) (*Agent, error) {
	agent := &Agent{
		esClient,
		phoenixClient,
		DefaultElasticTopic,
		DefaultElasticSize,
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
				err := agent.processGroups()
				if err != nil {
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

	for _, group := range groups {
		manager.ProcessGroup(agent.esClient, agent.phoenixClient, group, agent.esTopic, agent.esSize)
	}

	return nil
}
