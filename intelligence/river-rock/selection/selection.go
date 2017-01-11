package selection

import (
	"database/sql"
	"encoding/json"
	"errors"
	_ "github.com/lib/pq"
	"github.com/orcaman/concurrent-map"
	"log"
	"math/rand"
	"strconv"
)

const (
	sqlGetGroupId           = "select id from groups where scope=$1 and name=$2 limit 1"
	sqlGetFallbackClusterId = "select fallback_id from fallback_cluster where cluster_id=$1 limit 1"
	sqlGetMappedResources   = "select mapped from resource_map where cluster_id=$1 and res=$2 limit 1"
)

type Selector struct {
	resourceCache cmap.ConcurrentMap
	db            *sql.DB
}

func NewSelector(db *sql.DB) *Selector {
	return &Selector{
		resourceCache: cmap.New(),
		db:            db,
	}
}

func (s *Selector) parseResourceN(mappedResources []interface{}, selected int) (string, error) {

	ref := mappedResources[selected]

	switch ref.(type) {
	case string:
		return ref.(string), nil
	}

	return "", errors.New("unable to parse ref: " + strconv.Itoa(selected))
}

func (s *Selector) selectResourceFromArray(clusterId int, mappedResources []interface{}) (string, error) {
	sz := len(mappedResources)
	if sz == 0 {
		return "", errors.New("MappedResources array should not be empty")
	}

	//TODO: Implement Octo Fox multi armed bandit selection function

	selected := rand.Intn(sz)

	return s.parseResourceN(mappedResources, selected)
}

func (s *Selector) SelectResource(clusterId int, encodedMappedResources string) (string, error) {
	var mappedResources interface{}
	refBytes := []byte(encodedMappedResources)
	if err := json.Unmarshal(refBytes, &mappedResources); err != nil {
		log.Print(err)
		return "", err
	}

	switch mappedResources.(type) {
	case string:
		return mappedResources.(string), nil
	case []interface{}:
		return s.selectResourceFromArray(clusterId, mappedResources.([]interface{}))
	}
	return "", errors.New("unable to map mappedResources json to a valid type: " + encodedMappedResources)
}

func (s *Selector) getMappedResourcesRaw(clusterId int, res string) (string, error) {

	var mappedResources string

	stmt, err := s.db.Prepare(sqlGetMappedResources)
	if err != nil {
		return mappedResources, err
	}

	row := stmt.QueryRow(clusterId, res)
	if err := row.Scan(&mappedResources); err != nil {
		return mappedResources, err
	}

	return mappedResources, nil
}

func (s *Selector) getFallbackCluster(clusterId int) (int, error) {
	stmt, err := s.db.Prepare(sqlGetFallbackClusterId)
	if err != nil {
		return clusterId, err
	}

	fallbackClusterId := clusterId

	row := stmt.QueryRow(clusterId)
	if err := row.Scan(&fallbackClusterId); err != nil {
		return clusterId, err
	}

	return fallbackClusterId, nil
}

func (s *Selector) GetMappedResources(clusterId int, res string) (string, error) {

	mappedResources, err := s.getMappedResourcesRaw(clusterId, res)
	if err != nil {
		fallbackClusterId, err := s.getFallbackCluster(clusterId)
		if err != nil {
			return mappedResources, err
		}

		return s.getMappedResourcesRaw(fallbackClusterId, res)
	}

	return mappedResources, nil
}
