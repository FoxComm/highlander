package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/remote/responses"
	"github.com/FoxComm/highlander/remote/services"
	"github.com/FoxComm/highlander/remote/utils/failures"
)

// Ping is a really simple controller used for health checks.
type Ping struct {
	dbs *services.RemoteDBs
}

type health struct {
	Intelligence string `json:"intelligence"`
	Phoenix      string `json:"phoenix"`
}

// GetHealth tests the connection to the databases and returns the status.
func (ctrl *Ping) GetHealth() ControllerFunc {
	return func() (*responses.Response, failures.Failure) {
		icPingErr := ctrl.dbs.IC().Ping()
		phxPingErr := ctrl.dbs.Phx().Ping()

		statusCode := http.StatusOK
		h := health{
			Intelligence: "passed",
			Phoenix:      "passed",
		}

		if icPingErr != nil {
			statusCode = http.StatusInternalServerError
			h.Intelligence = "failed"
		}

		if phxPingErr != nil {
			statusCode = http.StatusInternalServerError
			h.Phoenix = "failed"
		}

		return responses.NewResponse(statusCode, h), nil
	}
}
