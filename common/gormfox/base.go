package gormfox

import "time"

// Our internal version of https://github.com/jinzhu/gorm/blob/master/model.go
// JSON tags allow for cleaner data when we marshal for client.
type Base struct {
	ID        uint       `gorm:"primary_key" json:"id"`
	CreatedAt time.Time  `json:"createdAt"`
	UpdatedAt time.Time  `json:"updatedAt"`
	DeletedAt *time.Time `json:"deletedAt"`
}
