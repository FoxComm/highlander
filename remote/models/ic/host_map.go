package ic

// HostMap stores the relationship been site hostnames and channels.
// Used by River Rock to determine which channel to use for a given request.
type HostMap struct {
	ID        int
	Host      string
	ChannelID int
	Scope     string
}
