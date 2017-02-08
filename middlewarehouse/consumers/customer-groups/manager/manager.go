package manager

import (
	"fmt"
	"log"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/shared/mailchimp"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"

	"gopkg.in/olivere/elastic.v3"
	"reflect"
)

type Customer struct {
	ID    int    `json:"id"`
	Email string `json:"email"`
}

func ProcessGroup(esClient *elastic.Client, phoenixClient phoenix.PhoenixClient, chimpClient *mailchimp.ChimpClient, group *responses.CustomerGroupResponse, topic string, size int, listID string) error {
	if group.GroupType == "manual" {
		log.Printf("Group %s with id %d is manual, skipping.\n", group.Name, group.ID)
	} else {
		go func(group *responses.CustomerGroupResponse) {
			customers, err := getCustomersEmails(esClient, group, topic, size)
			if err != nil {
				log.Panicf("An error occured getting customers: %s", err)
			}

			ids := getKeys(customers)

			if err := phoenixClient.SetGroupToCustomers(group.ID, ids); err != nil {
				log.Panicf("An error occured setting group to customers: %s", err)
			}

			if group.CustomersCount != len(ids) {
				if err := updateGroup(phoenixClient, group, len(ids)); err != nil {
					log.Panicf("An error occured update group info: %s", err)
				}
			}

			if err := processMailchimp(chimpClient, listID, group, customers); err != nil {
				log.Printf("An error occured updating mailchimp: %s", err.Error())
			}

		}(group)
	}

	return nil
}

func processMailchimp(chimpClient *mailchimp.ChimpClient, listID string, group *responses.CustomerGroupResponse, customers map[int]string) error {
	newEmails := getValues(customers)
	segments, err := chimpClient.GetSegments(listID)
	if err != nil {
		return fmt.Errorf("An error occured trying to get segments from mailchimp: %s", err)
	}

	log.Printf("Segments count: %d", segments.Total)

	var segmentToUpdate *mailchimp.SegmentResponse

	for i := 0; i < len(segments.Segments) && segmentToUpdate == nil; i++ {
		segment := segments.Segments[i]

		spl := strings.Split(segments.Segments[i].Name, "#")
		if len(spl) < 2 {
			return fmt.Errorf("Splitted segment name length should be gte 2. Ex: 12#CG Name. Actual name: %s", segment.Name)
		}

		id, err := strconv.Atoi(spl[0])
		if err != nil {
			return fmt.Errorf("Can't extract CG id from segments name %s", segment.Name)
		}

		if id == group.ID {
			segmentToUpdate = &segments.Segments[i]
		}
	}

	segmentPayload := &mailchimp.SegmentPayload{
		Name:          fmt.Sprintf("%d#%s", group.ID, group.Name),
		StaticSegment: newEmails,
	}

	if segmentToUpdate != nil {
		log.Printf("Found segment to update: %s. Checking if update is required...", segmentToUpdate.Name)

		members, err := chimpClient.GetSegmentMembers(listID, segmentToUpdate.ID)
		if err != nil {
			return err
		}

		oldEmails := make([]string, len(members.Members))
		for i, m := range members.Members {
			oldEmails[i] = m.Email
		}

		log.Printf("New name=%s; old name=%s", segmentPayload.Name, segmentToUpdate.Name)
		log.Printf("New emails len=%d; old emails len=%d; diff len=%d", len(newEmails), len(oldEmails), len(utils.DiffSlices(newEmails, oldEmails)))

		if segmentToUpdate.Name != segmentPayload.Name || len(newEmails) != len(oldEmails) || len(utils.DiffSlices(newEmails, oldEmails)) > 0 {
			_, err = chimpClient.UpdateStaticSegment(listID, segmentToUpdate.ID, segmentPayload)
		}
	} else {
		log.Printf("Not found segment to update for group %d#%s", group.ID, group.Name)

		_, err = chimpClient.CreateSegment(listID, segmentPayload)
	}

	if err != nil {
		return err
	}

	return nil
}

func getCustomersEmails(esClient *elastic.Client, group *responses.CustomerGroupResponse, topic string, size int) (map[int]string, error) {
	query := string(group.ElasticRequest)
	raw := elastic.RawStringQuery(query)

	from := 0
	done := false

	result := map[int]string{}

	for !done {
		log.Printf("Quering ES. From: %d, Size: %d, Query: %s", from, size, query)
		res, err := esClient.Search().Type(topic).Query(raw).Fields("id", "email").From(from).Size(size).Do()

		if err != nil {
			return nil, err
		}

		for _, hit := range res.Hits.Hits {
			id, err := getEsField(hit.Fields, "id")
			if err != nil {
				return nil, err
			}
			email, err := getEsField(hit.Fields, "email")
			if err != nil {
				return nil, err
			}

			result[int(id.(float64))] = email.(string)
		}

		from += size

		done = res.Hits.TotalHits <= int64(from)
	}

	return result, nil
}

func getEsField(esFields map[string]interface{}, fieldName string) (interface{}, error) {
	field, found := esFields[fieldName]
	if !found {
		return nil, fmt.Errorf("expected SearchResult.Hits.Hit.Fields[%s] to be found", fieldName)
	}
	fields, ok := field.([]interface{})
	if !ok {
		return nil, fmt.Errorf("expected []interface{}; got: %v", reflect.TypeOf(fields))
	}
	if len(fields) != 1 {
		return nil, fmt.Errorf("expected a field with 1 entry; got: %d", len(fields))
	}

	return fields[0], nil
}

func getKeys(mmap map[int]string) []int {
	keys := make([]int, len(mmap))

	i := 0
	for k := range mmap {
		keys[i] = k
		i++
	}
	return keys
}

func getValues(mmap map[int]string) []string {
	values := make([]string, len(mmap))

	i := 0
	for _, v := range mmap {
		values[i] = v
		i++
	}
	return values
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
