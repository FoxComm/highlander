package ic

// Channel is the representation of what a channel looks like in River Rock.
type Channel struct {
	ID             int
	OrganizationID int
}

func (c Channel) HostMaps(hosts []string, scope string) []*HostMap {
	hostMaps := make([]*HostMap, len(hosts))

	for idx, host := range hosts {
		hostMap := &HostMap{
			Host:      host,
			ChannelID: c.ID,
			Scope:     scope,
		}

		hostMaps[idx] = hostMap
	}

	return hostMaps
}
