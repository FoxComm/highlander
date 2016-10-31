package payloads

type Carrier struct {
	Name             string `json:"name" binding:"required"`
	TrackingTemplate string `json:"trackingTemplate" binding:"required"`
	Scopable
}
