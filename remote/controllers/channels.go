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
	icDB  *gorm.DB
	phxDB *gorm.DB
}

func NewChannels(icDB, phxDB *gorm.DB) *Channels {
	return &Channels{icDB: icDB, phxDB: phxDB}
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
		icChannel := payload.IntelligenceModel()
		phxChannel := payload.PhoenixModel()

		if err := services.InsertChannel(ctrl.icDB, ctrl.phxDB, icChannel, phxChannel, payload.Hosts); err != nil {
			return nil, err
		}

		resp := responses.NewChannel(phxChannel)
		return responses.NewResponse(http.StatusCreated, resp), nil
	}
}

// UpdateChannel updates an existing channel.
func (ctrl *Channels) UpdateChannel(id int, payload *payloads.UpdateChannel) ControllerFunc {
	return func() (*responses.Response, failures.Failure) {
		existingPhxChannel := &phoenix.Channel{}
		if err := services.FindChannelByID(ctrl.phxDB, id, existingPhxChannel); err != nil {
			return nil, err
		}

		phxChannel := payload.PhoenixModel(existingPhxChannel)
		if err := services.UpdateChannel(ctrl.phxDB, phxChannel); err != nil {
			return nil, err
		}

		resp := responses.NewChannel(phxChannel)
		return responses.NewResponse(http.StatusOK, resp), nil
	}
}
