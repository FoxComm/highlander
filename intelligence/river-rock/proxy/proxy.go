package proxy

import (
	"database/sql"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"

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

func (p *RiverRock) StartProxy() {

	bernardoUrl := p.Config.BernardoUrl + "/sfind"

	e := echo.New()

	e.GET("/v1/*", func(c echo.Context) error {
		req := c.Request()
		res := c.Response()

		path := req.URL.Path

		//TODO: Take request and consult bernardo about the cluster
		clusterId, err := clustering.MapRequestToCluster(req, bernardoUrl)

		mappedResources, err := selection.GetMappedResources(p.Db, clusterId, path)

		proxy := httputil.NewSingleHostReverseProxy(p.Upstream)

		if err != nil {
			log.Print(err)
			log.Print("PASS: " + path + " => " + p.Config.UpstreamUrl + path)
			proxy.ServeHTTP(res, req)
		} else {
			ref, err := selection.SelectResource(clusterId, mappedResources)
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

	e.GET("/raw/*", func(c echo.Context) error {
		req := c.Request()
		log.Print(req.URL.Path)
		return c.String(http.StatusOK, "STUFF HERE")
	})

	e.Logger.Fatal(e.Start(":" + p.Config.Port))
}
