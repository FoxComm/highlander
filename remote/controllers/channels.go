package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/payloads"
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

		resp := responses.NewChannel(channel)
		return responses.NewResponse(http.StatusOK, resp), nil
	}
}

// CreateChannel creates a new channel.
func (ctrl *Channels) CreateChannel(payload *payloads.CreateChannel) ControllerFunc {
	return func() (*responses.Response, failures.Failure) {
		phxChannel := payload.PhoenixModel()

		if err := services.InsertChannel(ctrl.phxDB, phxChannel); err != nil {
			return nil, err
		}

		resp := responses.NewChannel(phxChannel)
		return responses.NewResponse(http.StatusCreated, resp), nil
	}
}
