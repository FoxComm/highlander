package proxy

import (
	"database/sql"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strconv"

	"github.com/labstack/echo"
	//"net/http"
	"github.com/FoxComm/highlander/intelligence/river-rock/clustering"
	"github.com/FoxComm/highlander/intelligence/river-rock/selection"
	_ "github.com/lib/pq"
)

type ProxyConfig struct {
	DbConn      string
	UpstreamUrl string
	Port        string
	BernardoUrl string
}

type RiverRock struct {
	Config   *ProxyConfig
	Db       *sql.DB
	Upstream *url.URL
}

func NewProxy(c *ProxyConfig) (*RiverRock, error) {

	db, err := sql.Open("postgres", c.DbConn)
	if err != nil {
		return nil, err
	}

	upstream, err := url.Parse(c.UpstreamUrl)
	if err != nil {
		return nil, err
	}

	return &RiverRock{
		Config:   c,
		Db:       db,
		Upstream: upstream,
	}, nil
}

func getClusterIdFromHeader(req *http.Request) (int, error) {
	if clusterHeader, ok := req.Header["X-Cluster"]; ok {
		if len(clusterHeader) > 0 {
			return strconv.Atoi(clusterHeader[0])
		}
	}
	return -1, nil
}

func (p *RiverRock) StartProxy() error {

	bernardoUrl := p.Config.BernardoUrl + "/sfind"

	selector := selection.NewSelector(p.Db)

	e := echo.New()

	e.GET("/ping", func(c echo.Context) error {
		return c.String(http.StatusOK, "pong")
	})

	e.GET("/v1/*", func(c echo.Context) error {
		req := c.Request()
		res := c.Response()

		path := req.URL.Path

		//Get pinned cluster Id from header
		clusterId, err := getClusterIdFromHeader(req)

		var mappedResources string

		//If no clusterId is pinned, then map the request to a cluster using bernardo.
		if clusterId == -1 || err != nil {
			clusterId, err = clustering.MapRequestToCluster(req, bernardoUrl)
		}

		//If we have a cluster id, get the set of mapped resources
		if clusterId != -1 && err == nil {
			mappedResources, err = selector.GetMappedResources(clusterId, path)
		}

		proxy := httputil.NewSingleHostReverseProxy(p.Upstream)

		//If we have a cluster id, return it in a response header
		if clusterId != -1 {
			res.Header().Add("X-Cluster", strconv.Itoa(clusterId))
		}

		//If there was a problem with clustering or mapping, then just
		//proxy original resource via the upstream server
		//otherwise we will select a resource from the set and proxy that back
		//to the client.
		if err != nil {
			log.Print(err)
			log.Print("PASS: " + path + " => " + p.Config.UpstreamUrl + path)

			proxy.ServeHTTP(res, req)
		} else {
			ref, err := selector.SelectResource(clusterId, mappedResources)
			if err != nil {
				log.Print(err)
			} else {
				req.URL.Path = ref
			}

			log.Print("MAP: " + path + " => " + p.Config.UpstreamUrl + ref)
			proxy.ServeHTTP(res, req)
		}

		return nil
	})

	return e.Start(":" + p.Config.Port)
}
