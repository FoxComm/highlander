package mailchimp

import "fmt"

const (
	EndpointListsStaticSegments string = "/lists/%s/segments"
	EndpointListsStaticSegment  string = "/lists/%s/segments/%d"
)

func (c *ChimpClient) StaticSegments(listID string) (StaticSegmentsResponse, error) {
	var response StaticSegmentsResponse
	err := c.get(fmt.Sprintf(EndpointListsStaticSegments+"?count=60", listID), &response)
	return response, err
}

func (c *ChimpClient) CreateStaticSegment(listID string, payload *StaticSegmentPayload) (StaticSegmentResponse, error) {
	var response StaticSegmentResponse
	err := c.post(fmt.Sprintf(EndpointListsStaticSegments, listID), payload, &response)
	return response, err
}

func (c *ChimpClient) UpdateStaticSegment(listID string, segmentId int, payload *StaticSegmentPayload) (StaticSegmentResponse, error) {
	var response StaticSegmentResponse
	err := c.patch(fmt.Sprintf(EndpointListsStaticSegment, listID, segmentId), payload, &response)
	return response, err
}

func (c *ChimpClient) DeleteStaticSegment(listID string, segmentId int) error {
	return c.delete(fmt.Sprintf(EndpointListsStaticSegment, listID, segmentId))
}

type StaticSegmentPayload struct {
	Name          string   `json:"name"`
	StaticSegment []string `json:"static_segment"`
}

type LinkResponse struct {
	Rel          string
	Href         string
	Method       string
	TargetSchema string
	Schema       string
}

type StaticSegmentResponse struct {
	ID           int            `json:"id"`
	Name         string         `json:"name"`
	MembersCount int            `json:"member_count"`
	Type         string         `json:"type"`
	CreatedAt    string         `json:"created_at"`
	UpdatedAt    string         `json:"updated_at"`
	ListID       string         `json:"list_id"`
	Links        []LinkResponse `json:"_links"`
}

type StaticSegmentsResponse struct {
	Segments []StaticSegmentResponse `json:"segments"`
	ListID   string                  `json:"list_id"`
	Total    int                     `json:"total_items"`
	Links    []LinkResponse          `json:"_links"`
}
