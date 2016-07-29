package responses

type GiftCardTransaction struct {
	ID       uint   `json:"id"`
	Code     string `json:"code"`
	Amount   uint   `json:"amount"`
	PlacedAt string `json:"placedAt"`
}
