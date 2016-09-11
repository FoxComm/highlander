package lib

type LoginPayload struct {
	Email    string `binding:"required"`
	Kind     string `binding:"required"`
	Password string `binding:"required"`
}
