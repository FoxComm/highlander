package phoenix

import "time"

// Channel represents an avenue for purchasing on the Fox Platform. This could
// be a website (theperfectgourmet.com), third-party (Amazon), or sale type (B2B).
type Channel struct {
	ID               int
	Name             string
	PurchaseLocation int
	CreatedAt        time.Time
	UpdatedAt        time.Time
}
