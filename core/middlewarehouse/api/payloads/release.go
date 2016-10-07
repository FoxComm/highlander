package payloads

type Release struct {
	RefNum string `json:"refNum" binding:"required"`
}
