package manager

import (
	"errors"
	"fmt"
	"log"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/shared/mailchimp"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"

	"gopkg.in/olivere/elastic.v3"
)

const (
	DefaultElasticIndex    = "admin"
	DefaultElasticTopic    = "customers_search_view"
	DefaultElasticSize     = 100
	DefaultMailchimpListID = ""
	DefaultTimeout         = 30 * time.Minute
)

type GroupsManager struct {
	esClient      *elastic.Client
	phoenixClient phoenix.PhoenixClient
	chimpClient   *mailchimp.ChimpClient
	esTopic       string
	esSize        int
	chimpListID   string
	timeout       time.Duration
}

type ManagerOptionFunc func(*GroupsManager)

func SetTimeout(t time.Duration) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.timeout = t
	}
}

func SetElasticTopic(topic string) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.esTopic = topic
	}
}

func SetElasticQierySize(size int) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.esSize = size
	}
}

func SetMailchimpListID(id string) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.chimpListID = id
	}
}

func NewGroupsManager(esClient *elastic.Client, phoenixClient phoenix.PhoenixClient, chimpClient *mailchimp.ChimpClient, options ...ManagerOptionFunc) *GroupsManager {
	manager := &GroupsManager{
		esClient,
		phoenixClient,
		chimpClient,
		DefaultElasticTopic,
		DefaultElasticSize,
		DefaultMailchimpListID,
		DefaultTimeout,
	}

	// set options to manager
	for _, opt := range options {
		opt(manager)
	}

	return manager
}

func (m *GroupsManager) ProcessChangedGroup(group *responses.CustomerGroupResponse) error {
	go func(group *responses.CustomerGroupResponse) {
		// get customers associated with the group - map of [id]:email
		customers, err := m.getCustomers(group)
		if err != nil {
			log.Panicf("An error occured getting customers: %s", err.Error())
		}

		// update group-customers mapping for dynamic groups
		if group.GroupType != "manual" {
			if err := m.updateGroupCustomersMapping(group, customers); err != nil {
				log.Panicf("An error occured updating group-customers mapping: %s", err)
			}
		}

		// update segments(fc "groups" analogue) in mailchimp
		if err := m.processMailchimp(group, customers); err != nil {
			log.Printf("An error occured updating mailchimp: %s", err.Error())
		}
	}(group)

	return nil
}

func (m *GroupsManager) ProcessDeletedGroup(group *responses.CustomerGroupResponse) error {
	listID := m.chimpListID

	segments, err := m.chimpClient.GetSegments(listID)
	if err != nil {
		return fmt.Errorf("An error occured trying to get segments from mailchimp: %s", err)
	}

	groupSegment, err := findSegmentByGroup(group, segments)
	if err != nil {
		return err
	}

	if groupSegment != nil {
		return m.chimpClient.DeleteStaticSegment(listID, groupSegment.ID)
	}

	return nil
}

func (m *GroupsManager) updateGroupCustomersMapping(group *responses.CustomerGroupResponse, customers map[int]string) error {
	ids := getKeys(customers)

	if err := m.phoenixClient.SetCustomersToGroup(group.ID, ids); err != nil {
		log.Panicf("An error occured setting group to customers: %s", err)
	}

	if group.CustomersCount != len(ids) {
		updateGroup := &payloads.CustomerGroupPayload{group.Name, group.GroupType, len(ids), group.ClientState, group.ElasticRequest}

		if err := m.phoenixClient.UpdateCustomerGroup(group.ID, updateGroup); err != nil {
			log.Panicf("An error occured update group info: %s", err)
		}
	}

	return nil
}

func (m *GroupsManager) processMailchimp(group *responses.CustomerGroupResponse, customers map[int]string) error {
	listID := m.chimpListID
	if listID == "" {
		return errors.New("Please provide not empty mailchimp ListId value")
	}

	newEmails := getValues(customers)

	// get available segments from mailchimp
	segments, err := m.chimpClient.GetSegments(listID)
	if err != nil {
		return fmt.Errorf("An error occured trying to get segments from mailchimp: %s", err)
	}

	// find segment that was create for this group by `{groupID}#{groupName}` naming pattern
	groupSegment, err := findSegmentByGroup(group, segments)
	if err != nil {
		return err
	}

	segmentPayload := &mailchimp.SegmentPayload{
		Name:          fmt.Sprintf("%d#%s", group.ID, group.Name),
		StaticSegment: newEmails,
	}
	if groupSegment != nil {
		// get list of segment members
		members, err := m.chimpClient.GetSegmentMembers(listID, groupSegment.ID)
		if err != nil {
			return err
		}

		oldEmails := make([]string, len(members.Members))
		for i, m := range members.Members {
			oldEmails[i] = m.Email
		}

		// update segment members if the name of group changed or list of customers of group differs from segment members
		if groupSegment.Name != segmentPayload.Name || len(newEmails) != len(oldEmails) || len(utils.DiffSlices(newEmails, oldEmails)) > 0 {
			_, err = m.chimpClient.UpdateStaticSegment(listID, groupSegment.ID, segmentPayload)
		}
	} else {
		// create new segment if no segment was found for this group
		_, err = m.chimpClient.CreateSegment(listID, segmentPayload)
	}

	if err != nil {
		return err
	}

	return nil
}

func (m *GroupsManager) getCustomers(group *responses.CustomerGroupResponse) (map[int]string, error) {
	var query elastic.Query

	if group.GroupType == "manual" {
		query = elastic.NewTermQuery("groups", group.ID)
	} else {
		query = elastic.RawStringQuery(group.ElasticRequest)
	}

	topic := m.esTopic
	size := m.esSize
	from := 0
	done := false

	result := map[int]string{}

	for !done {
		log.Printf("Quering ES. From: %d, Size: %d, Query: %s", from, size, query)

		res, err := m.esClient.
			Search().
			Type(topic).
			Query(query).
			Fields("id", "email").
			From(from).
			Size(size).
			Do()

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
