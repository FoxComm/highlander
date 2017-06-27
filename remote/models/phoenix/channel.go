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
	Name             string
	PurchaseLocation int
	CreatedAt        time.Time
	UpdatedAt        time.Time
}

func NewChannel(name string, purchaseLocation int) *Channel {
	return &Channel{
		ID:               0,
		Name:             name,
		PurchaseLocation: purchaseLocation,
		CreatedAt:        time.Now().UTC(),
		UpdatedAt:        time.Now().UTC(),
	}
}

func (c Channel) Table() string {
	return "channels"
}

func (c Channel) Fields() map[string]interface{} {
	return map[string]interface{}{
		"name":              c.Name,
		"purchase_location": c.PurchaseLocation,
		"created_at":        c.CreatedAt,
		"updated_at":        c.UpdatedAt,
	}
}

func (c Channel) FieldRefs() []interface{} {
	return []interface{}{
		&c.ID,
		&c.Name,
		&c.PurchaseLocation,
		&c.CreatedAt,
		&c.UpdatedAt,
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
