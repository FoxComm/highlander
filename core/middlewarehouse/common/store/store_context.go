package store

import "github.com/gin-gonic/gin"

// StoreContext defines information about the tenant that may be passed
// throughout the application.
type StoreContext struct {
	StoreID int
}

// NewContent takes the Gin context and constructs an internal context
// that's used to identify the tenant.
func NewStoreContext(ctx *gin.Context) (*StoreContext, error) {
	return &StoreContext{StoreID: 1}, nil
}
