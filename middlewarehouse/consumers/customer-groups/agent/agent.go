package agent

import (
	"log"
	"strconv"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
	elastic "gopkg.in/olivere/elastic.v3"
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
		go func(group responses.CustomerGroupResponse) {
			ids, err := agent.getCustomersIDs(group)
			if err != nil {
				log.Panicf("An error occured getting customers: %s", err)
			}

			if err := agent.phoenixClient.SetGroupToCustomers(group.ID, ids); err != nil {
				log.Panicf("An error occured setting group to customers: %s", err)
			}

			if group.CustomersCount != len(ids) {
				if err := agent.updateGroup(group, len(ids)); err != nil {
					log.Panicf("An error occured update group info: %s", err)
				}
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
		res, err := agent.esClient.Search().Type(agent.esTopic).Query(raw).Fields().From(from).Size(agent.esSize).Do()

		if err != nil {
			return nil, err
		}

		for _, hit := range res.Hits.Hits {
			customerId, err := strconv.Atoi(hit.Id)
			if err != nil {
				return nil, err
			}

			if !ids[customerId] {
				ids[customerId] = true
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
	updateGroup := &payloads.CustomerGroupPayload{
		Name:           group.Name,
		CustomersCount: customersCount,
		ClientState:    group.ClientState,
		ElasticRequest: group.ElasticRequest,
	}

	return agent.phoenixClient.UpdateCustomerGroup(group.ID, updateGroup)
}
