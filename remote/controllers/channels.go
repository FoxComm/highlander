package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/responses"
	"github.com/FoxComm/highlander/remote/services"
	"github.com/FoxComm/highlander/remote/utils/failures"
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
	return func() (*responses.Response, failures.Failure) {
		channel := &phoenix.Channel{}

		if err := services.FindChannelByID(ctrl.phxDB, id, channel); err != nil {
			return nil, err
		}

		chResp := &responses.Channel{}
		chResp.Build(channel)

		return &responses.Response{
			StatusCode: http.StatusOK,
			Body:       chResp,
		}, nil
	}
}

// CreateChannel creates a new channel.
// func (ctrl *Channels) CreateChannel(payload *payloads.CreateChannel) ControllerFunc {
// 	return func() *responses.Response {
// 		return &responses.Response{
// 			StatusCode: http.StatusCreated,
// 			Body:       "I'm a channel",
// 		}
// 	}
// }
