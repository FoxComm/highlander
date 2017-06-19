package payloads

import (
	"time"

	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/utils/failures"
)

// CreateChannel is the structure of payload needed to create a channel.
type CreateChannel struct {
	Name          string `json:"name"`
	PurchaseOnFox *bool  `json:"purchaseOnFox"`
	CatalogID     *int64 `json:"catalogId"`
}

// Validate ensures that the has the correct format.
func (c CreateChannel) Validate() failures.Failure {
	if c.Name == "" {
		return failures.NewFieldEmptyFailure("name")
	} else if c.PurchaseOnFox == nil {
		return failures.NewFieldEmptyFailure("purchaseOnFox")
	}

	return nil
}

// PhoenixModel returns the phoenix model for this payload.
func (c CreateChannel) PhoenixModel() *phoenix.Channel {
	model := &phoenix.Channel{
		Name:      c.Name,
		CreatedAt: time.Now().UTC(),
		UpdatedAt: time.Now().UTC(),
	}

	if *(c.PurchaseOnFox) {
		model.PurchaseLocation = phoenix.PurchaseOnFox
	} else {
		model.PurchaseLocation = phoenix.PurchaseOffFox
	}

	return model
}

// UpdateChannel is the structure of payload needed to update a channel.
type UpdateChannel struct {
	Name          *string `json:"name"`
	PurchaseOnFox *bool   `json:"purchaseOnFox"`
	CatalogID     *int64  `json:"catalogId"`
}

// Validate ensures that they payload has the correct format.
// For this payload, it's making sure that there's at least one value.
func (c UpdateChannel) Validate() failures.Failure {
	if c.Name == nil && c.PurchaseOnFox == nil && c.CatalogID == nil {
		return failures.NewEmptyPayloadFailure()
	} else if c.Name != nil && (*c.Name) == "" {
		return failures.NewFieldEmptyFailure("name")
	}

	return nil
}

func (c UpdateChannel) PhoenixModel(existing *phoenix.Channel) *phoenix.Channel {
	newPhxChannel := phoenix.Channel{
		ID:        existing.ID,
		CreatedAt: existing.CreatedAt,
		UpdatedAt: time.Now().UTC(),
	}

	if c.Name != nil {
		newPhxChannel.Name = *(c.Name)
	} else {
		newPhxChannel.Name = existing.Name
	}

	if c.PurchaseOnFox != nil {
		if *(c.PurchaseOnFox) {
			newPhxChannel.PurchaseLocation = phoenix.PurchaseOnFox
		} else {
			newPhxChannel.PurchaseLocation = phoenix.PurchaseOffFox
		}
	} else {
		newPhxChannel.PurchaseLocation = existing.PurchaseLocation
	}

	return &newPhxChannel
}
