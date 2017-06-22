package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/remote/models/ic"
	"github.com/FoxComm/highlander/remote/models/phoenix"
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
		icChannel := &ic.Channel{}
		phxChannel := &phoenix.Channel{}

		if err := services.FindChannelByID(ctrl.dbs, id, icChannel, phxChannel); err != nil {
			return nil, err
		}

		resp := responses.NewChannel(icChannel, phxChannel, []string{})
		return responses.NewResponse(http.StatusOK, resp), nil
	}
}

// CreateChannel creates a new channel.
func (ctrl *Channels) CreateChannel(payload *payloads.CreateChannel) ControllerFunc {
	return func() (*responses.Response, failures.Failure) {
		icChannel := payload.IntelligenceModel()
		phxChannel := payload.PhoenixModel()

		if err := services.InsertChannel(ctrl.dbs, icChannel, phxChannel, payload.Hosts); err != nil {
			return nil, err
		}

		resp := responses.NewChannel(icChannel, phxChannel, payload.Hosts)
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
