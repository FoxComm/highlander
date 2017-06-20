package phoenix

import "time"

type Organization struct {
	ID        int
	Name      string
	Kind      string
	ParentID  int
	ScopeID   int
	CreatedAt time.Time
	UpdatedAt time.Time
	DeletedAt *time.Time
}
