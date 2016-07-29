package responses

type StoreCreditTransaction struct {
	ID       uint   `json:"id"`
	Amount   uint   `json:"amount"`
	PlacedAt string `json:"placedAt"`
}
