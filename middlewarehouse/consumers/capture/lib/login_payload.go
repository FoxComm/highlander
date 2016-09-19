package lib

type LoginPayload struct {
	Email    string `json:"email" binding:"required"`
	Kind     string `json:"kind" binding:"required"`
	Password string `json:"password" binding:"required"`
}
