package phoenix

import (
	"errors"
	"fmt"
	"time"
)

// Channel represents an avenue for purchasing on the Fox Platform. This could
// be a website (theperfectgourmet.com), third-party (Amazon), or sale type (B2B).
type Channel struct {
	ID               int
	Scope            string
	Name             string
	PurchaseLocation int
	CreatedAt        time.Time
	UpdatedAt        time.Time
}

func NewChannel(scope string, name string, purchaseLocation int) *Channel {
	return &Channel{
		ID:               0,
		Scope:            scope,
		Name:             name,
		PurchaseLocation: purchaseLocation,
		CreatedAt:        time.Now().UTC(),
		UpdatedAt:        time.Now().UTC(),
	}
}

func (c Channel) Validate() error {
	if c.Name == "" {
		return errors.New("Channel name must not be empty")
	} else if c.PurchaseLocation != PurchaseOnFox && c.PurchaseLocation != PurchaseOffFox {
		return fmt.Errorf(
			"Expected PurchaseLocation to be %d or %d, got %d",
			PurchaseOnFox,
			PurchaseOffFox,
			c.PurchaseLocation)
	}

	return nil
}
