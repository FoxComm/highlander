package phoenix

import "time"

// Channel represents an avenue for purchasing on the Fox Platform. This could
// be a website (theperfectgourmet.com), third-party (Amazon), or sale type (B2B).
type Channel struct {
	ID               int64
	Name             string
	PurchaseLocation PurchaseLocation
	CreatedAt        time.Time
	UpdatedAt        time.Time
}
