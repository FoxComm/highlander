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

func (c *Channel) Build(phxChannel *phoenix.Channel) {
	c.ID = phxChannel.ID
	c.Name = phxChannel.Name
	c.CreatedAt = phxChannel.CreatedAt
	c.UpdatedAt = phxChannel.UpdatedAt

	if phxChannel.PurchaseLocation == phoenix.PurchaseOnFox {
		c.PurchaseOnFox = true
	} else {
		c.PurchaseOnFox = false
	}
}
