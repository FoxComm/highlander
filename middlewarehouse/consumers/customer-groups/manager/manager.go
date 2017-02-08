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

	"errors"
	"gopkg.in/olivere/elastic.v3"
	"reflect"
)

func ProcessChangedGroup(esClient *elastic.Client, phoenixClient phoenix.PhoenixClient, chimpClient *mailchimp.ChimpClient, group *responses.CustomerGroupResponse, topic string, size int, listID string) error {
	go func(group *responses.CustomerGroupResponse) {
		// get customers associated with the group - map of [id]:email
		customers, err := getCustomers(esClient, group, topic, size)
		if err != nil {
			log.Panicf("An error occured getting customers: %s", err.Error())
		}

		// update group-customers mapping for dynamic groups
		if group.GroupType != "manual" {
			if err := updateGroupCustomersMapping(phoenixClient, group, customers); err != nil {
				log.Panicf("An error occured updating group-customers mapping: %s", err)
			}
		}

		// update segments(fc "groups" analogue) in mailchimp
		if err := processMailchimp(chimpClient, listID, group, customers); err != nil {
			log.Printf("An error occured updating mailchimp: %s", err.Error())
		}
	}(group)

	return nil
}

func ProcessDeletedGroup(chimpClient *mailchimp.ChimpClient, group *responses.CustomerGroupResponse, listID string) error {
	segments, err := chimpClient.GetSegments(listID)
	if err != nil {
		return fmt.Errorf("An error occured trying to get segments from mailchimp: %s", err)
	}

	groupSegment, err := findSegmentByGroup(group, segments)
	if err != nil {
		return err
	}

	if groupSegment != nil {
		return chimpClient.DeleteStaticSegment(listID, groupSegment.ID)
	}

	return nil
}

func updateGroupCustomersMapping(phoenixClient phoenix.PhoenixClient, group *responses.CustomerGroupResponse, customers map[int]string) error {
	ids := getKeys(customers)

	if err := phoenixClient.SetCustomersToGroup(group.ID, ids); err != nil {
		log.Panicf("An error occured setting group to customers: %s", err)
	}

	if group.CustomersCount != len(ids) {
		updateGroup := &payloads.CustomerGroupPayload{group.Name, group.GroupType, len(ids), group.ClientState, group.ElasticRequest}

		if err := phoenixClient.UpdateCustomerGroup(group.ID, updateGroup); err != nil {
			log.Panicf("An error occured update group info: %s", err)
		}
	}

	return nil
}

func processMailchimp(chimpClient *mailchimp.ChimpClient, listID string, group *responses.CustomerGroupResponse, customers map[int]string) error {
	if listID == "" {
		return errors.New("Please provide not empty mailchimp ListId value")
	}

	newEmails := getValues(customers)
	segments, err := chimpClient.GetSegments(listID)
	if err != nil {
		return fmt.Errorf("An error occured trying to get segments from mailchimp: %s", err)
	}

	groupSegment, err := findSegmentByGroup(group, segments)
	if err != nil {
		return err
	}

	segmentPayload := &mailchimp.SegmentPayload{
		Name:          fmt.Sprintf("%d#%s", group.ID, group.Name),
		StaticSegment: newEmails,
	}
	if groupSegment != nil {
		members, err := chimpClient.GetSegmentMembers(listID, groupSegment.ID)
		if err != nil {
			return err
		}

		oldEmails := make([]string, len(members.Members))
		for i, m := range members.Members {
			oldEmails[i] = m.Email
		}

		if groupSegment.Name != segmentPayload.Name || len(newEmails) != len(oldEmails) || len(utils.DiffSlices(newEmails, oldEmails)) > 0 {
			_, err = chimpClient.UpdateStaticSegment(listID, groupSegment.ID, segmentPayload)
		}
	} else {
		_, err = chimpClient.CreateSegment(listID, segmentPayload)
	}

	if err != nil {
		return err
	}

	return nil
}

func getCustomers(esClient *elastic.Client, group *responses.CustomerGroupResponse, topic string, size int) (map[int]string, error) {
	var query elastic.Query

	if group.GroupType == "manual" {
		query = elastic.NewTermQuery("groups", group.ID)
	} else {
		query = elastic.RawStringQuery(group.ElasticRequest)
	}

	from := 0
	done := false

	result := map[int]string{}

	for !done {
		log.Printf("Quering ES. From: %d, Size: %d, Query: %s", from, size, query)

		res, err := esClient.Search().Type(topic).Query(query).Fields("id", "email").From(from).Size(size).Do()

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

func findSegmentByGroup(group *responses.CustomerGroupResponse, segments *mailchimp.SegmentsResponse) (*mailchimp.SegmentResponse, error) {
	for i := 0; i < len(segments.Segments); i++ {
		segment := segments.Segments[i]

		spl := strings.Split(segments.Segments[i].Name, "#")
		if len(spl) < 2 {
			return nil, fmt.Errorf("Splitted segment name length should be gte 2. Ex: 12#CG Name. Actual name: %s", segment.Name)
		}

		id, err := strconv.Atoi(spl[0])
		if err != nil {
			return nil, fmt.Errorf("Can't extract CG id from segments name %s", segment.Name)
		}

		if id == group.ID {
			return &segments.Segments[i], nil
		}
	}

	return nil, nil
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
