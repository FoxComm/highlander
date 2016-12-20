package payloads

type ShippingAddress struct {
	Address
	IsDefault   bool   `json:"isDefault" binding:"required"`
	PhoneNumber string `json:"phoneNumber" binding:"required"`
}
