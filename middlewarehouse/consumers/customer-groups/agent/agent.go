package agent

import (
    "log"
    "time"

    "github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/manager"
    "github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
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
        if group.Type == "manual" {
            log.Printf("Group %s with id %d is manual, skipping.\n", group.Name, group.ID)
        }

        if group.Type == "dynamic" {
            log.Printf("Group %s with id %d is dynamic, processing.\n", group.Name, group.ID)

            go func(group *responses.CustomerGroupResponse) {
                ids, err := manager.GetCustomersIDs(agent.esClient, group, agent.esTopic, agent.esSize)
                if err != nil {
                    log.Panicf("An error occured getting customers: %s", err)
                }

                if err := agent.phoenixClient.SetGroupToCustomers(group.ID, ids); err != nil {
                    log.Panicf("An error occured setting group to customers: %s", err)
                }

                if group.CustomersCount != len(ids) {
                    if err := manager.UpdateGroup(agent.phoenixClient, group, len(ids)); err != nil {
                        log.Panicf("An error occured update group info: %s", err)
                    }
                }
            }(group)
        }
    }

    return nil
}
