package common

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// FoxError is an interface for wrapping errors in GoLang that will assist with
// logging and reporting errors to the customer.
type FoxError interface {
	Error() string
	SetResponse(c *gin.Context)
}

// NewNotFoundError creates a new error when an entity isn't found.
func NewNotFoundError(err error) FoxError {
	return &notFoundError{err}
}

// NewBadRequestError creates a new error when an action occurs because of an
// incomplete or invalid request by an outside system or user.
func NewBadRequestError(err error) FoxError {
	return &badRequestError{err}
}

// NewUnhandledError creates a new error when an action fails that we expect to
// have succeeded.
func NewUnhandledError(err error) FoxError {
	return &unhandledError{err}
}

type notFoundError struct {
	err error
}

func (nf notFoundError) Error() string {
	return nf.err.Error()
}

func (nf notFoundError) SetResponse(c *gin.Context) {
	c.AbortWithError(http.StatusNotFound, nf.err)
}

type badRequestError struct {
	err error
}

func (br badRequestError) Error() string {
	return br.err.Error()
}

func (br badRequestError) SetResponse(c *gin.Context) {
	c.AbortWithError(http.StatusBadRequest, br.err)
}

type unhandledError struct {
	err error
}

func (ue unhandledError) Error() string {
	return ue.err.Error()
}

func (ue unhandledError) SetResponse(c *gin.Context) {
	c.AbortWithError(http.StatusInternalServerError, ue.err)
}
