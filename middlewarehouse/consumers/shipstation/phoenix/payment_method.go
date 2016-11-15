package phoenix

type PaymentMethod struct {
	ID         int      `json:"id" binding:"required"`
	CustomerID int      `json:"customerId" binding:"required"`
	Type       string   `json:"type" binding:"required"`
	Brand      *string  `json:"brand"`
	Address    *Address `json:"address"`
	ExpYear    *int     `json:"expYear"`
	ExpMonth   *int     `json:"expMonth"`
	LastFour   *string  `json:"lastFour"`
	Code       *string  `json:"code"`
	HolderName string   `json:"holderName"`
}
