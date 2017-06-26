package channels

import (
	"database/sql"
	"errors"
	"log"
	"net/http"
	"strconv"

	_ "github.com/lib/pq"
	"github.com/orcaman/concurrent-map"
)

const (
	sqlHostMapping = "select channel_id, scope from host_map where host=$1 limit 1"
)

type Channels struct {
	Db        *sql.DB
	HostCache cmap.ConcurrentMap
}

type HostMapping struct {
	ChannelId int
	Scope     string
}

func NewChannels(db *sql.DB) (*Channels, error) {
	if db == nil {
		return nil, errors.New("Database connection handler is nil")
	}

	return &Channels{
		Db:        db,
		HostCache: cmap.New(),
	}, nil
}

func (c *Channels) AddChannelHeaders(req *http.Request) {
	mapping, err := c.LookupHost(req.Host)
	if mapping == nil {
		return
	}

	if err != nil {
		log.Printf("Database error: %s", err.Error())
		return
	}

	req.Header.Add("X-Channel", strconv.Itoa(mapping.ChannelId))
	req.Header.Add("X-Scope", mapping.Scope)
}

func (c *Channels) LookupHost(host string) (*HostMapping, error) {
	// Get from cache
	v, found := c.HostCache.Get(host)
	if found {
		return v.(*HostMapping), nil
	}

	hostMap := HostMapping{}

	// Get from database
	row := c.Db.QueryRow(sqlHostMapping, host)
	if err := row.Scan(&hostMap.ChannelId, &hostMap.Scope); err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}

		return nil, err
	}

	// Put it to cache
	c.HostCache.Set(host, &hostMap)

	// Return
	return &hostMap, nil
}
