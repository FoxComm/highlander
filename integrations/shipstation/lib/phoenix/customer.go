package phoenix

// Customer represents the representation of the customer that is part of an order.
type Customer struct {
	ID            int    `json:"id" binding:"required"`
	Name          string `json:"name" binding:"required"`
	Email         string `json:"email" binding:"required"`
	IsGuest       bool   `json:"isGuest" binding:"required"`
	Disabled      bool   `json:"disabled" binding:"required"`
	IsBlacklisted bool   `json:"isBlacklisted" binding:"required"`
}
