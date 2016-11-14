package utils

import (
	"encoding/json"
	"net/http"
)

type ElasticServer struct{}

func (es *ElasticServer) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Path

	switch url {
	case "/public/products_catalog_view/_search":
		es.handleSearch(w, r)
	}
}

func (es *ElasticServer) handleSearch(w http.ResponseWriter, r *http.Request) {
	something := map[string]string{
		"Hello": "World",
	}

	respBytes, _ := json.Marshal(something)
	w.Write(respBytes)
}
