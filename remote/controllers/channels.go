package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/payloads"
	"github.com/FoxComm/highlander/remote/responses"
)

// GetChannel finds a single channel by its ID.
func GetChannel(id int) ControllerFunc {
	return func() *responses.Response {
		channel := phoenix.Channel{
			ID:               1,
			Name:             "The Perfect Gourmet",
			PurchaseLocation: 1,
		}

		return &responses.Response{
			StatusCode: http.StatusOK,
			Body:       channel,
		}
	}
}

// CreateChannel creates a new channel.
func CreateChannel(payload *payloads.CreateChannel) ControllerFunc {
	return func() *responses.Response {
		return &responses.Response{
			StatusCode: http.StatusCreated,
			Body:       "I'm a channel",
		}
	}
}
