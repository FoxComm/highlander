package main

import (
	"errors"
	"fmt"
	"log"
	"os"
	//"net/http"
	"github.com/FoxComm/highlander/intelligence/river-rock/proxy"
	_ "github.com/lib/pq"
)

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

	bernardoUrl := os.Getenv("BERNARDO_URL")
	if bernardoUrl == "" {
		return nil, errors.New("BERNARDO_URL is not set")
	}

	port := os.Getenv("PORT")
	if port == "" {
		return nil, errors.New("PORT is not set")
	}

	return &proxy.ProxyConfig{
		DbConn:      conn,
		UpstreamUrl: upstreamUrl,
		Port:        port,
		BernardoUrl: bernardoUrl,
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

	proxy.StartProxy()
}
