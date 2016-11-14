package mocks

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"

	"github.com/FoxComm/highlander/consumers/product-index/search"
	"github.com/FoxComm/highlander/shared/golang/elastic"
)

type ElasticServer struct {
	index   string
	mapping string
	Rows    map[string]search.SearchRow
}

func NewElasticServer(index string, mapping string, rows map[string]search.SearchRow) *ElasticServer {
	return &ElasticServer{index, mapping, rows}
}

func (es *ElasticServer) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Path
	method := r.Method

	if method == "POST" && url == es.searchURL() {
		es.handleSearch(w, r)
	} else if method == "PUT" && strings.HasPrefix(url, es.updateURL()) {
		es.handleUpdate(w, r)
	} else if method == "DELETE" && strings.HasPrefix(url, es.updateURL()) {
		es.handleDelete(w, r)
	} else {
		log.Printf("Unexpected URL %s with method %s", url, method)
		es.handleNotFound(w, r)
	}
}

func (es *ElasticServer) searchURL() string {
	return fmt.Sprintf("/%s/%s/_search", es.index, es.mapping)
}

func (es *ElasticServer) updateURL() string {
	return fmt.Sprintf("/%s/%s", es.index, es.mapping)
}

func (es *ElasticServer) handleSearch(w http.ResponseWriter, r *http.Request) {
	hits := elastic.Hits{
		Total:    len(es.Rows),
		MaxScore: 1.0,
	}

	for id, row := range es.Rows {
		hit := elastic.Hit{
			Index:  es.index,
			ID:     id,
			Score:  1.0,
			Source: row,
		}

		hits.Hits = append(hits.Hits, hit)
	}
	result := elastic.Result{
		Took:     5,
		TimedOut: false,
		Hits:     hits,
	}

	respBytes, err := json.Marshal(result)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
	} else {
		w.Write(respBytes)
	}
}

func (es *ElasticServer) handleUpdate(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	row := new(search.SearchRow)

	if err := json.NewDecoder(r.Body).Decode(row); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
	}

	idx := r.URL.Path[len(es.updateURL())+1:]
	es.Rows[idx] = *row

	w.WriteHeader(http.StatusOK)
}

func (es *ElasticServer) handleDelete(w http.ResponseWriter, r *http.Request) {
	idx := r.URL.Path[len(es.updateURL())+1:]
	delete(es.Rows, idx)
}

func (es *ElasticServer) handleNotFound(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotFound)
}
