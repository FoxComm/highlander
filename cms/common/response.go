package common

import "github.com/gin-gonic/gin"

// FoxResponse is an interface for wrapping non-error responses.
type FoxResponse interface {
	SetResponse(c *gin.Context)
	StatusCode() int
	Value() interface{}
}

// NewFoxResponse creates a new FoxResponse object.
func NewFoxResponse(statusCode int, value interface{}) FoxResponse {
	return &foxResponse{statusCode, value}
}

type foxResponse struct {
	statusCode int
	value      interface{}
}

func (r foxResponse) SetResponse(c *gin.Context) {
	c.JSON(r.statusCode, r.value)
}

func (r foxResponse) StatusCode() int {
	return r.statusCode
}

func (r foxResponse) Value() interface{} {
	return r.value
}
