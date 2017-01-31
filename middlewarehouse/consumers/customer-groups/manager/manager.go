package manager

import (
	"log"
	"strconv"

	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"

	"gopkg.in/olivere/elastic.v3"
)

func GetCustomersIDs(esClient *elastic.Client, group *responses.CustomerGroupResponse, topic string, size int) ([]int, error) {
	query := string(group.ElasticRequest)
	raw := elastic.RawStringQuery(query)

	from := 0
	done := false

	ids := map[int]bool{}

	for !done {
		//log.Printf("Quering ES. From: %d, Size: %d, Query: %s", from, size, query)
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

		log.Printf("Queried ES. From: %d, Size: %d, TotalHits: %d, Query: %s", from, size, res.Hits.TotalHits, query)

		from += size

		done = res.Hits.TotalHits <= int64(from)
	}

	result := []int{}
	for key := range ids {
		result = append(result, key)
	}

	return result, nil
}

func UpdateGroup(phoenixClient phoenix.PhoenixClient, group *responses.CustomerGroupResponse, customersCount int) error {
	updateGroup := &payloads.CustomerGroupPayload{
		Name:           group.Name,
		GroupType:      group.GroupType,
		CustomersCount: customersCount,
		ClientState:    group.ClientState,
		ElasticRequest: group.ElasticRequest,
	}

	return phoenixClient.UpdateCustomerGroup(group.ID, updateGroup)
}
