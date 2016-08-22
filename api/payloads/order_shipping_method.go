package payloads

type OrderShippingMethod struct {
	ID        uint   `json:"id" binding:"required"`
	Name      string `json:"name" binding:"required"`
	Price     int    `json:"price"`
	IsEnabled bool   `json:"isEnabled"`
}
