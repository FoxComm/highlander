package responses

type GiftCardTransaction struct {
	ID        uint   `json:"id"`
	Code      string `json:"code"`
	Amount    uint   `json:"amount"`
	CreatedAt string `json:"createdAt"`
}
