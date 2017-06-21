package manager

import (
	"errors"
	"fmt"
	"io"
	"log"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/shared/mailchimp"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"

	elastic "gopkg.in/olivere/elastic.v3"
	"encoding/json"
)

const (
	DefaultElasticIndex    = "admin_1"
	DefaultElasticTopic    = "customers_search_view"
	DefaultElasticSize     = 100
	DefaultMailchimpListID = ""
)

type GroupsManager struct {
	esClient      *elastic.Client
	phoenixClient phoenix.PhoenixClient
	chimpClient   *mailchimp.ChimpClient
	esIndex       string
	esTopic       string
	esSize        int
	chimpListID   string
	chimpDisabled bool
}

type ManagerOptionFunc func(*GroupsManager)

type Customer struct {
	Id  int `json:"id"`
	Email string `json:"email"`
}

func SetMailchimpListID(id string) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.chimpListID = id
	}
}

func SetMailchimpDisabled(disabled bool) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.chimpDisabled = disabled
	}
}

func SetElasticIndex(index string) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.esIndex = index
	}
}

func SetElasticQuerySize(size int) ManagerOptionFunc {
	return func(m *GroupsManager) {
		m.esSize = size
	}
}

func NewGroupsManager(esClient *elastic.Client, phoenixClient phoenix.PhoenixClient, chimpClient *mailchimp.ChimpClient, options ...ManagerOptionFunc) *GroupsManager {
	manager := &GroupsManager{
		esClient,
		phoenixClient,
		chimpClient,
		DefaultElasticIndex,
		DefaultElasticTopic,
		DefaultElasticSize,
		DefaultMailchimpListID,
		false,
	}

	// set options to manager
	for _, opt := range options {
		opt(manager)
	}

	return manager
}

func (m *GroupsManager) ProcessChangedGroup(group *responses.CustomerGroupResponse) error {
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
		log.Printf("An error occured updating segments in mailchimp: %s", err.Error())
	}

	return nil
}

func (m *GroupsManager) ProcessDeletedGroup(group *responses.CustomerGroupResponse) error {
	if m.chimpDisabled {
		return errors.New("Mailchimp integration is disabled")
	}

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
	if m.chimpDisabled {
		return errors.New("Mailchimp integration is disabled")
	}

	listID := m.chimpListID

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
	query := elastic.RawStringQuery(group.ElasticRequest)
	index := m.esIndex
	topic := m.esTopic
	size := m.esSize

	result := map[int]string{}

	log.Printf("Scrolling ES. Index: %s, Name: %s, Size: %d, Query: %s", index, group.Name, size, query)

	scroll := m.esClient.
		Scroll().
		Index(index).
		Type(topic).
		Query(query).
		Size(size)

	for {
		res, err := scroll.Do()
		if err == io.EOF {
			break
		}

		if err != nil {
			return nil, err
		}

		for _, hit := range res.Hits.Hits {
			var c Customer
			err := json.Unmarshal(*hit.Source, &c)

			if err != nil {
				return nil, err
			}

			result[c.Id] = c.Email
		}
	}

	scroll.Clear(nil)

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
