package responses

import (
	"time"

	"github.com/FoxComm/highlander/remote/models/phoenix"
)

type Channel struct {
	ID            int       `json:"id"`
	Name          string    `json:"name"`
	PurchaseOnFox bool      `json:"purchaseOnFox"`
	CreatedAt     time.Time `json:"createdAt"`
	UpdatedAt     time.Time `json:"updatedAt"`
}

func NewChannel(phxChannel *phoenix.Channel) *Channel {
	c := Channel{
		ID:        phxChannel.ID,
		Name:      phxChannel.Name,
		CreatedAt: phxChannel.CreatedAt,
		UpdatedAt: phxChannel.UpdatedAt,
	}

	if phxChannel.PurchaseLocation == phoenix.PurchaseOnFox {
		c.PurchaseOnFox = true
	} else {
		c.PurchaseOnFox = false
	}

	return &c
}
