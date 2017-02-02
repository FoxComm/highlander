package manager

import (
	"log"
	"strconv"

	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"

	"gopkg.in/olivere/elastic.v3"
)

func ProcessGroup(esClient *elastic.Client, phoenixClient phoenix.PhoenixClient, group *responses.CustomerGroupResponse, topic string, size int) error {
	if group.GroupType == "manual" {
		log.Printf("Group %s with id %d is manual, skipping.\n", group.Name, group.ID)
	} else {
		go func(group *responses.CustomerGroupResponse) {
			ids, err := getCustomersIDs(esClient, group, topic, size)
			if err != nil {
				log.Panicf("An error occured getting customers: %s", err)
			}

			if err := phoenixClient.SetGroupToCustomers(group.ID, ids); err != nil {
				log.Panicf("An error occured setting group to customers: %s", err)
			}

			if group.CustomersCount != len(ids) {
				if err := updateGroup(phoenixClient, group, len(ids)); err != nil {
					log.Panicf("An error occured update group info: %s", err)
				}
			}
		}(group)
	}

	return nil
}

func getCustomersIDs(esClient *elastic.Client, group *responses.CustomerGroupResponse, topic string, size int) ([]int, error) {
	query := string(group.ElasticRequest)
	raw := elastic.RawStringQuery(query)

	from := 0
	done := false

	ids := map[int]bool{}

	for !done {
		log.Printf("Quering ES. From: %d, Size: %d, Query: %s", from, size, query)
		res, err := esClient.Search().Type(topic).Query(raw).Fields().From(from).Size(size).Do()

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

		from += size

		done = res.Hits.TotalHits <= int64(from)
	}

	result := []int{}
	for key := range ids {
		result = append(result, key)
	}

	return result, nil
}

func updateGroup(phoenixClient phoenix.PhoenixClient, group *responses.CustomerGroupResponse, customersCount int) error {
	updateGroup := &payloads.CustomerGroupPayload{
		Name:           group.Name,
		GroupType:      group.GroupType,
		CustomersCount: customersCount,
		ClientState:    group.ClientState,
		ElasticRequest: group.ElasticRequest,
	}

	return phoenixClient.UpdateCustomerGroup(group.ID, updateGroup)
}
