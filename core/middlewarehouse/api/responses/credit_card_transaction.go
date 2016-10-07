package responses

type CreditCardTransaction struct {
	ID         uint   `json:"id"`
	Brand      string `json:"brand"`
	HolderName string `json:"holderName"`
	LastFour   string `json:"lastFour"`
	ExpMonth   uint   `json:"expMonth"`
	ExpYear    uint   `json:"expYear"`
	Amount     uint   `json:"amount"`
	CreatedAt  string `json:"createdAt"`
}
