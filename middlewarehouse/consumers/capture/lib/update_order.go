package lib

import (
	"fmt"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

const (
	orderStateOrdered            = "ordered"
	orderStateFraudHold          = "fraudHold"
	orderStateRemorseHold        = "remorseHold"
	orderStateManualHold         = "manualHold"
	orderStateCanceled           = "canceled"
	orderStateFulfillmentStarted = "fulfillmentStarted"
	orderStateShipped            = "shipped"
)

type UpdateOrderPayload struct {
	State string `json:"state"`
}

func NewUpdateOrderPayload(state string) (*UpdateOrderPayload, exceptions.IException) {
	switch state {
	case orderStateOrdered:
	case orderStateFraudHold:
	case orderStateRemorseHold:
	case orderStateManualHold:
	case orderStateCanceled:
	case orderStateFulfillmentStarted:
	case orderStateShipped:
		return &UpdateOrderPayload{state}, nil
	}

	return nil, NewCaptureClientException(fmt.Errorf("Order state %s is not valid", state))
}
