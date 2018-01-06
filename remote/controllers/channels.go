package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/remote/payloads"
	"github.com/FoxComm/highlander/remote/responses"
	"github.com/FoxComm/highlander/remote/services"
	"github.com/FoxComm/highlander/remote/utils/failures"
)

type Channels struct {
	dbs *services.RemoteDBs
}

func NewChannels(dbs *services.RemoteDBs) *Channels {
	return &Channels{dbs: dbs}
}

// GetChannel finds a single channel by its ID.
func (ctrl *Channels) GetChannel(id int) ControllerFunc {
	return func() (*responses.Response, failures.Failure) {
		resp, fail := services.FindChannelByID(ctrl.dbs, id)
		if fail != nil {
			return nil, fail
		}

		return responses.NewResponse(http.StatusOK, resp), nil
	}
}

// CreateChannel creates a new channel.
func (ctrl *Channels) CreateChannel(payload *payloads.CreateChannel) ControllerFunc {
	return func() (*responses.Response, failures.Failure) {
		resp, fail := services.InsertChannel(ctrl.dbs, payload)
		if fail != nil {
			return nil, fail
		}

		return responses.NewResponse(http.StatusCreated, resp), nil
	}
}

// UpdateChannel updates an existing channel.
func (ctrl *Channels) UpdateChannel(id int, payload *payloads.UpdateChannel) ControllerFunc {
	return func() (*responses.Response, failures.Failure) {

		// existingPhxChannel := &phoenix.Channel{}
		// if err := services.FindChannelByID(ctrl.dbs, id, existingPhxChannel); err != nil {
		// 	return nil, err
		// }

		// phxChannel := payload.PhoenixModel(existingPhxChannel)
		// if err := services.UpdateChannel(ctrl.dbs, phxChannel); err != nil {
		// 	return nil, err
		// }

		// resp := responses.NewChannel(phxChannel)
		// return responses.NewResponse(http.StatusOK, resp), nil
		return nil, nil
	}
}
