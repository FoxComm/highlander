package phoenix

import "time"

// Site represents the web presence of a place where produts in a channel are
// purchased. It also maintains the relationship between a channel and the
// catalog that contains the products.
type Site struct {
	ID        int64
	Name      string
	Host      string
	CatalogID int64
	ChannelID int64
	CreatedAt time.Time
	UpdatedAt time.Time
}
