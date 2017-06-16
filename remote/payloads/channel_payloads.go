package payloads

import "github.com/FoxComm/highlander/remote/models/phoenix"

// CreateChannel is the structure of payload needed to create a channel.
type CreateChannel struct {
	Name          string `json:"name"`
	PurchaseOnFox bool   `json:"purchaseOnFox"`
	CatalogID     *int64 `json:"catalogId"`
}

// PhoenixModel returns the phoenix model for this payload.
func (c CreateChannel) PhoenixModel() *phoenix.Channel {
	model := &phoenix.Channel{Name: c.Name}

	if c.PurchaseOnFox {
		model.PurchaseLocation = phoenix.PurchaseOnFox
	} else {
		model.PurchaseLocation = phoenix.PurchaseOffFox
	}

	return model
}
