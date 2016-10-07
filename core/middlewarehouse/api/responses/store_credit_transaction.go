package responses

type StoreCreditTransaction struct {
	ID        uint   `json:"id"`
	Amount    uint   `json:"amount"`
	CreatedAt string `json:"createdAt"`
}
