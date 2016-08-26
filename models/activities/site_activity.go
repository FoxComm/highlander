package activities

// SiteActivity is as action that occurs within the system that should be
// written to the activity log.
type SiteActivity interface {
	Type() string
	Data() string
	CreatedAt() string
}
