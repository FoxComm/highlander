package proxy

import (
	"crypto/tls"
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
	"github.com/FoxComm/highlander/intelligence/river-rock/utils"
	_ "github.com/lib/pq"
)

type ProxyConfig struct {
	DbConn       string
	UpstreamUrl  string
	Port         string
	BernardoHost string
	BernardoUrl  string
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

func getBernardoUrl(c *ProxyConfig) (string, error) {
	//lookup bernardo host and port using SRV records
	if c.BernardoUrl == "" {
		bernardoHost, bernardoPort, err := utils.LookupHostAndPort(c.BernardoHost)
		if err != nil {
			return "", err
		}
		return "http://" + bernardoHost + ":" + bernardoPort + "/sfind", nil
	}
	return c.BernardoUrl, nil
}

func selectResource(selector *selection.Selector, clusterId int, originalPath string, mappedResources string) (bool, string, error) {
	mappedPath, err := selector.SelectResource(clusterId, mappedResources)
	if err != nil {
		return false, originalPath, err
	} else if mappedPath == "" {
		return false, originalPath, nil
	}
	return true, mappedPath, nil
}

func (p *RiverRock) StartProxy() error {

	//Turn of SSL verification since we hit the balancer via internal endpoint
	http.DefaultTransport.(*http.Transport).TLSClientConfig = &tls.Config{InsecureSkipVerify: true}

	selector := selection.NewSelector(p.Db)

	e := echo.New()

	e.GET("/ping", func(c echo.Context) error {
		return c.String(http.StatusOK, "pong")
	})

	e.Any("/proxy/*", func(c echo.Context) error {
		req := c.Request()
		res := c.Response()

		//strip out /proxy
		originalPath := req.URL.Path[6:]
		reqUri := req.RequestURI[6:]

		//Get pinned cluster Id from header
		clusterId, err := getClusterIdFromHeader(req)

		var mappedResources string

		//If no clusterId is pinned, then map the request to a cluster using bernardo.
		if clusterId == -1 || err != nil {
			bernardoUrl, err := getBernardoUrl(p.Config)
			if err == nil {
				clusterId, err = clustering.MapRequestToCluster(req, bernardoUrl)
			}
		}

		//If we have a cluster id, get the set of mapped resources
		if clusterId != -1 && err == nil {
			mappedResources, err = selector.GetMappedResources(clusterId, reqUri)
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
			log.Printf("CLUSTER ERROR: %v : %v", req.URL.RequestURI(), err)
			req.URL.Path = originalPath
		} else if clusterId == -1 {
			req.URL.Path = originalPath
		} else {
			mapped, mappedPath, err := selectResource(selector, clusterId, originalPath, mappedResources)
			if err != nil {
				log.Printf("SELECT ERROR: %v : %v", req.URL.RequestURI(), err)
			} else if mapped {
				log.Print("MAP: " + reqUri + " => " + p.Config.UpstreamUrl + mappedPath)
			}
			req.URL.Path = mappedPath
		}

		req.RequestURI = req.URL.RequestURI()
		proxy.ServeHTTP(res, req)

		return nil
	})

	return e.Start(":" + p.Config.Port)
}
