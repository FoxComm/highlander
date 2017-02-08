package agent

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/manager"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
)

const (
	DefaultTimeout = 30 * time.Minute
)

type Agent struct {
	phoenixClient phoenix.PhoenixClient
	manager       *manager.GroupsManager
	timeout       time.Duration
}

type AgentOptionFunc func(*Agent)

func SetTimeout(t time.Duration) AgentOptionFunc {
	return func(a *Agent) {
		a.timeout = t
	}
}

func NewAgent(phoenixClient phoenix.PhoenixClient, groupsManager *manager.GroupsManager, options ...AgentOptionFunc) *Agent {
	agent := &Agent{
		phoenixClient,
		groupsManager,
		DefaultTimeout,
	}

	// set options to agent
	for _, opt := range options {
		opt(agent)
	}

	return agent
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

	for _, group := range groups {
		agent.manager.ProcessChangedGroup(group)
	}

	return nil
}
