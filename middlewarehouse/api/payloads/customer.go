package payloads

// Customer represents the representation of the customer that is part of an order.
type Customer struct {
	ID            int    `json:"id" binding:"required"`
	Name          string `json:"name" binding:"required"`
	Email         string `json:"email" binding:"required"`
	IsGuest       bool   `json:"isGuest"`
	Disabled      bool   `json:"disabled"`
	IsBlacklisted bool   `json:"isBlacklisted"`
}
