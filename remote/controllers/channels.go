package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/payloads"
	"github.com/FoxComm/highlander/remote/responses"
	"github.com/FoxComm/highlander/remote/services"
	"github.com/jinzhu/gorm"
)

type Channels struct {
	phxDB *gorm.DB
}

func NewChannels(phxDB *gorm.DB) *Channels {
	return &Channels{phxDB: phxDB}
}

// GetChannel finds a single channel by its ID.
func (ctrl *Channels) GetChannel(id int) ControllerFunc {
	return func() *responses.Response {
		channel := &phoenix.Channel{}

		err := services.FindChannelByID(ctrl.phxDB, id, channel)
		if err != nil {
			if err.Error() == "record not found" {
				return &responses.Response{
					StatusCode: http.StatusNotFound,
					Errs:       []error{err},
				}
			}

			return &responses.Response{
				StatusCode: http.StatusInternalServerError,
				Errs:       []error{err},
			}
		}

		chResp := &responses.Channel{}
		chResp.Build(channel)

		return &responses.Response{
			StatusCode: http.StatusOK,
			Body:       chResp,
		}
	}
}

// CreateChannel creates a new channel.
func (ctrl *Channels) CreateChannel(payload *payloads.CreateChannel) ControllerFunc {
	return func() *responses.Response {
		return &responses.Response{
			StatusCode: http.StatusCreated,
			Body:       "I'm a channel",
		}
	}
}
