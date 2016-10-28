package payloads

type Address struct {
	ID          uint    `json:"id"`
	Name        string  `json:"name"`
	Region      Region  `json:"region" binding:"required"`
	City        string  `json:"city" binding:"required"`
	Zip         string  `json:"zip" binding:"required"`
	Address1    string  `json:"address1" binding:"required"`
	Address2    *string `json:"address2"`
	PhoneNumber string  `json:"phoneNumber"`
}
