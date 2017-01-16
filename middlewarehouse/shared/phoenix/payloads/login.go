package payloads

type LoginPayload struct {
	Email    string `json:"email" binding:"required"`
	Org      string `json:"org" binding:"required"`
	Password string `json:"password" binding:"required"`
}
