package responses

import (
	"time"

	"github.com/FoxComm/highlander/remote/models/ic"
	"github.com/FoxComm/highlander/remote/models/phoenix"
)

type Channel struct {
	ID             int       `json:"id"`
	OrganizationID int       `json:"organizationId"`
	Name           string    `json:"name"`
	PurchaseOnFox  bool      `json:"purchaseOnFox"`
	Hosts          []string  `json:"hosts"`
	CreatedAt      time.Time `json:"createdAt"`
	UpdatedAt      time.Time `json:"updatedAt"`
}

func NewChannel(icChannel *ic.Channel, phxChannel *phoenix.Channel, hostMaps []*ic.HostMap) *Channel {
	hosts := make([]string, len(hostMaps))
	for idx, hostMap := range hostMaps {
		hosts[idx] = hostMap.Host
	}

	c := Channel{
		ID:             phxChannel.ID,
		OrganizationID: icChannel.OrganizationID,
		Name:           phxChannel.Name,
		Hosts:          hosts,
		CreatedAt:      phxChannel.CreatedAt,
		UpdatedAt:      phxChannel.UpdatedAt,
	}

	if phxChannel.PurchaseLocation == phoenix.PurchaseOnFox {
		c.PurchaseOnFox = true
	} else {
		c.PurchaseOnFox = false
	}

	return &c
}
