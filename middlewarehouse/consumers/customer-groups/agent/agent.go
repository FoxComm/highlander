package agent

import (
	"encoding/json"
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
	elastic "gopkg.in/olivere/elastic.v3"
)

const (
	DefaultTimeout         = 30 * time.Minute
	DefaultPhoenixURL      = "http:/127.0.0.1:9090"
	DefaultPhoenixUser     = "api"
	DefaultPhoenixPassword = "password"
	DefaultElasticURL      = "http://127.0.0.1:9200"
	DefaultElasticIndex    = "admin"
	DefaultElasticTopic    = "customers_search_view"
	DefaultElasticSize     = 100
)

type Customer struct {
	ID    int
	Name  string
	Email string
}

type Agent struct {
	esClient      *elastic.Client
	phoenixClient phoenix.PhoenixClient
	esURL         string
	esIndex       string
	esTopic       string
	esSize        int
	phoenixURL    string
	phoenixUser   string
	phoenixPass   string
	timeout       time.Duration
}

type AgentOptionFunc func(*Agent)

func SetTimeout(t time.Duration) AgentOptionFunc {
	return func(a *Agent) {
		a.timeout = t
	}
}

func SetElasticURL(url string) AgentOptionFunc {
	return func(a *Agent) {
		a.esURL = url
	}
}

func SetElasticIndex(index string) AgentOptionFunc {
	return func(a *Agent) {
		a.esIndex = index
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

func SetPhoenixURL(url string) AgentOptionFunc {
	return func(a *Agent) {
		a.phoenixURL = url
	}
}

func SetPhoenixAuth(user, password string) AgentOptionFunc {
	return func(a *Agent) {
		a.phoenixUser = user
		a.phoenixPass = password
	}
}

func NewAgent(options ...AgentOptionFunc) (*Agent, error) {
	agent := &Agent{
		nil,
		nil,
		DefaultElasticURL,
		DefaultElasticIndex,
		DefaultElasticTopic,
		DefaultElasticSize,
		DefaultPhoenixURL,
		DefaultPhoenixUser,
		DefaultPhoenixPassword,
		DefaultTimeout,
	}

	// set options to agent
	for _, opt := range options {
		opt(agent)
	}

	esClient, err := elastic.NewClient(
		elastic.SetURL(agent.esURL),
	)
	if err != nil {
		return nil, err
	}

	agent.esClient = esClient
	agent.phoenixClient = phoenix.NewPhoenixClient(agent.phoenixURL, agent.phoenixUser, agent.phoenixPass)

	return agent, nil
}

func (agent *Agent) Run() {
	log.Print("Running customer-groups agent")

	ticker := time.NewTicker(agent.timeout)

	for {
		select {
		case <-ticker.C:
			err := agent.processGroups()
			if err != nil {
				log.Panicf("An error occured processing groups: %s", err)
			}
		}
	}
}

func (agent *Agent) processGroups() error {
	groups, err := agent.phoenixClient.GetCustomerGroups()
	if err != nil {
		return err
	}

	for _, group := range groups {
		go func(group responses.CustomerGroupResponse) {
			ids, err := agent.getCustomersIDs(group)
			if err != nil {
				log.Panicf("An error occured getting customers: %s", err)
			}

			if err := agent.phoenixClient.SetGroupToCustomers(group.ID, ids); err != nil {
				log.Panicf("An error occured setting group to customers: %s", err)
			}

			if err := agent.updateGroup(group, len(ids)); err != nil {
				log.Panicf("An error occured update group info: %s", err)
			}
		}(group)
	}

	return nil
}

func (agent *Agent) getCustomersIDs(group responses.CustomerGroupResponse) ([]int, error) {
	query := string(group.ElasticRequest)
	raw := elastic.RawStringQuery(query)

	from := 0
	done := false

	ids := map[int]bool{}

	for !done {
		log.Printf("Quering ES. From: %d, Size: %d, Query: %s", from, agent.esSize, query)
		res, err := agent.esClient.Search().Type(agent.esTopic).Query(raw).From(from).Size(agent.esSize).Do()

		if err != nil {
			return nil, err
		}

		if res.Hits.TotalHits > 0 {
			for _, hit := range res.Hits.Hits {
				var customer Customer
				err := json.Unmarshal(*hit.Source, &customer)
				if err != nil {
					return nil, err
				}

				if !ids[customer.ID] {
					ids[customer.ID] = true
				}
			}
		}

		from += agent.esSize

		done = res.Hits.TotalHits <= int64(from)
	}

	result := []int{}
	for key := range ids {
		result = append(result, key)
	}

	return result, nil
}

func (agent *Agent) updateGroup(group responses.CustomerGroupResponse, customersCount int) error {
	updateGroup := &payloads.UpdateCustomerGroupPayload{
		Name:           group.Name,
		CustomersCount: customersCount,
		ClientState:    group.ClientState,
		ElasticRequest: group.ElasticRequest,
	}

	return agent.phoenixClient.UpdateCustomerGroup(group.ID, updateGroup)
}
