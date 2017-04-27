package mailchimp

import (
	"encoding/json"
	"fmt"
)

const (
	EndpointListsStaticSegments       string = "/lists/%s/segments"
	EndpointListsStaticSegment        string = "/lists/%s/segments/%d"
	EndpointListsStaticSegmentMembers string = "/lists/%s/segments/%d/members"
)

func (c *ChimpClient) GetSegments(listID string) (*SegmentsResponse, error) {
	response := new(SegmentsResponse)
	err := c.get(fmt.Sprintf(EndpointListsStaticSegments+"?count=60", listID), response)
	return response, err
}

func (c *ChimpClient) CreateSegment(listID string, payload *SegmentPayload) (*SegmentResponse, error) {
	response := new(SegmentResponse)
	err := c.post(fmt.Sprintf(EndpointListsStaticSegments, listID), payload, response)
	return response, err
}

func (c *ChimpClient) UpdateStaticSegment(listID string, segmentId int, payload *SegmentPayload) (*SegmentResponse, error) {
	response := new(SegmentResponse)
	err := c.patch(fmt.Sprintf(EndpointListsStaticSegment, listID, segmentId), payload, response)
	return response, err
}

func (c *ChimpClient) DeleteStaticSegment(listID string, segmentId int) error {
	return c.delete(fmt.Sprintf(EndpointListsStaticSegment, listID, segmentId))
}

func (c *ChimpClient) GetSegmentMembers(listID string, segmentId int) (*SegmentMembersResponse, error) {
	response := new(SegmentMembersResponse)
	err := c.get(fmt.Sprintf(EndpointListsStaticSegmentMembers+"?count=60", listID, segmentId), response)
	return response, err
}

type SegmentOptions struct {
	Match      string            `json:"match"`
	Conditions []json.RawMessage `json:"conditions"`
}

type SegmentPayload struct {
	Name          string          `json:"name"`
	StaticSegment []string        `json:"static_segment"`
	Options       *SegmentOptions `json:"options,omitempty"`
}

type LinkResponse struct {
	Rel          string
	Href         string
	Method       string
	TargetSchema string
	Schema       string
}

type SegmentResponse struct {
	ID           int            `json:"id"`
	Name         string         `json:"name"`
	MembersCount int            `json:"member_count"`
	Type         string         `json:"type"`
	CreatedAt    string         `json:"created_at"`
	UpdatedAt    string         `json:"updated_at"`
	ListID       string         `json:"list_id"`
	Links        []LinkResponse `json:"_links"`
}

type SegmentsResponse struct {
	Segments []SegmentResponse `json:"segments"`
	ListID   string            `json:"list_id"`
	Total    int               `json:"total_items"`
	Links    []LinkResponse    `json:"_links"`
}

type SegmentMemberResponse struct {
	ID    string `json:"id"`
	Email string `json:"email_address"`
}

type SegmentMembersResponse struct {
	Members []SegmentMemberResponse `json:"members"`
	Total   int                     `json:"total_items"`
	Links   []LinkResponse          `json:"_links"`
}
