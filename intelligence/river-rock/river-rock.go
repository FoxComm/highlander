package main

import (
	"errors"
	"fmt"
	"github.com/FoxComm/highlander/intelligence/river-rock/proxy"
	_ "github.com/lib/pq"
	"log"
	"net"
	"os"
	"strconv"
)

func lookupSrv(host string) (string, string, error) {
	_, srvs, err := net.LookupSRV("", "", host)
	if err != nil {
		return host, "", err
	}

	if len(srvs) == 0 {
		return host, "", errors.New("Unable to find port for " + host)
	}

	srv := srvs[0]

	host = srv.Target
	port := strconv.Itoa(int(srv.Port))

	return host, port, nil
}

func loadConfig() (*proxy.ProxyConfig, error) {
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

	bernardoHost := os.Getenv("BERNARDO_HOST")
	if bernardoHost == "" {
		return nil, errors.New("BERNARDO_HOST is not set")
	}

	bernardoPort := os.Getenv("BERNARDO_PORT")
	bernardoUrl := ""
	if len(bernardoPort) > 0 {
		bernardoUrl = "http://" + bernardoHost + ":" + bernardoPort + "/sfind"
	}

	log.Print("Bernardo host: " + bernardoHost)
	log.Print("Bernardo port: " + bernardoPort)

	port := os.Getenv("PORT")
	if port == "" {
		return nil, errors.New("PORT is not set")
	}

	return &proxy.ProxyConfig{
		DbConn:       conn,
		UpstreamUrl:  upstreamUrl,
		Port:         port,
		BernardoHost: bernardoHost,
		BernardoUrl:  bernardoUrl,
	}, nil
}

func main() {

	config, err := loadConfig()
	if err != nil {
		log.Fatal(err)
	}

	proxy, err := proxy.NewProxy(config)
	if err != nil {
		log.Fatal(err)
	}

	log.Fatal(proxy.StartProxy())
}
