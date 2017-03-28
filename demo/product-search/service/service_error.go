package service

import "github.com/gin-gonic/gin"

type ServiceError struct {
	StatusCode int
	Error      error
}

func (s ServiceError) Response(c *gin.Context) {
	errs := map[string][]string{"errors": []string{s.Error.Error()}}
	c.JSON(s.StatusCode, errs)
}
