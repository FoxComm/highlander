package main

import (
	"errors"
	"fmt"
	"log"
	"os"
	"strconv"
	//"net/http"
	"database/sql"
	"encoding/json"
	"github.com/labstack/echo"
	_ "github.com/lib/pq"
	"math/rand"
	"net/http"
	"net/http/httputil"
	"net/url"
)

const (
	sqlGetRefs = "select refs from resource_map where cluster_id=$1 and res=$2 limit 1"
)

func getRefs(db *sql.DB, clusterId int, res string) (string, error) {

	var refs string

	stmt, err := db.Prepare(sqlGetRefs)
	if err != nil {
		return refs, err
	}

	row := stmt.QueryRow(clusterId, res)
	if err := row.Scan(&refs); err != nil {
		return refs, err
	}

	return refs, nil
}

func parseRefN(refs []interface{}, selected int) (string, error) {

	ref := refs[selected]

	switch ref.(type) {
	case string:
		return ref.(string), nil
	}

	return "", errors.New("unable to parse ref: " + strconv.Itoa(selected))
}

func selectRefFromArray(clusterId int, refs []interface{}) (string, error) {
	sz := len(refs)
	if sz == 0 {
		return "", errors.New("Refs array should not be empty")
	}

	//TODO: Implement Octo Fox multi armed bandit selection function

	selected := rand.Intn(sz)

	return parseRefN(refs, selected)
}

func selectRef(clusterId int, encodedRefs string) (string, error) {
	var refs interface{}
	refBytes := []byte(encodedRefs)
	if err := json.Unmarshal(refBytes, &refs); err != nil {
		log.Print(err)
		return "", err
	}

	switch refs.(type) {
	case string:
		return refs.(string), nil
	case []interface{}:
		return selectRefFromArray(clusterId, refs.([]interface{}))
	}
	return "", errors.New("unable to map refs json to a valid type: " + encodedRefs)
}

type Config struct {
	DbConn      string
	UpstreamUrl string
	Port        string
}

func loadConfig() (*Config, error) {
	dbName := os.Getenv("DB_NAME")
	if dbName == "" {
		return nil, errors.New("DB_NAME is not set")
	}

	dbHost := os.Getenv("DB_HOST")
	if dbHost == "" {
		return nil, errors.New("DB_HOST is not set")
	}

	dbUser := os.Getenv("DB_USER")
	if dbUser == "" {
		return nil, errors.New("DB_USER is not set")
	}

	conn := fmt.Sprintf("host=%s user=%s dbname=%s sslmode=disable", dbHost, dbUser, dbName)

	dbPass := os.Getenv("DB_PASSWORD")
	if dbPass != "" {
		conn = fmt.Sprintf("password%s %s", dbPass, conn)
	}

	upstreamUrl := os.Getenv("UPSTREAM_URL")
	if upstreamUrl == "" {
		return nil, errors.New("UPSTREAM_URL is not set")
	}

	port := os.Getenv("PORT")
	if port == "" {
		return nil, errors.New("PORT is not set")
	}

	return &Config{
		DbConn:      conn,
		UpstreamUrl: upstreamUrl,
		Port:        port,
	}, nil
}

func main() {

	config, err := loadConfig()

	db, err := sql.Open("postgres", config.DbConn)
	if err != nil {
		log.Fatal(err)
	}

	upstream, err := url.Parse(config.UpstreamUrl)
	if err != nil {
		log.Fatal(err)
	}

	e := echo.New()

	e.GET("/v1/*", func(c echo.Context) error {
		req := c.Request()
		res := c.Response()

		path := req.URL.Path

		//TODO: Take request and consult bernardo about the cluster
		clusterId := 1

		refs, err := getRefs(db, clusterId, path)

		proxy := httputil.NewSingleHostReverseProxy(upstream)

		if err != nil {
			log.Print(err)
			log.Print("PASS: " + path + " => " + config.UpstreamUrl + path)
			proxy.ServeHTTP(res, req)
		} else {
			ref, err := selectRef(clusterId, refs)
			if err != nil {
				log.Print(err)
			} else {
				req.URL.Path = ref
			}

			log.Print("MAP: " + path + " => " + config.UpstreamUrl + ref)

			proxy.ServeHTTP(res, req)
		}

		return nil
	})

	e.GET("/raw/*", func(c echo.Context) error {
		req := c.Request()
		log.Print(req.URL.Path)
		return c.String(http.StatusOK, req.URL.Path)
	})

	e.Logger.Fatal(e.Start(":" + config.Port))
}
