package payloads

type OrderShippingMethod struct {
	ID        uint   `json:"id" binding:"required"`
	Name      string `json:"name" binding:"required"`
	Price     int    `json:"price" binding:"required"`
	IsEnabled bool   `json:"isEnabled" binding:"required"`
}
