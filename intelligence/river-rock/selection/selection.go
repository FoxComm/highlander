package selection

import (
	"database/sql"
	"encoding/json"
	"errors"
	"log"
	"math/rand"
	"strconv"
	//"net/http"
	_ "github.com/lib/pq"
)

const (
	sqlGetMappedResources = "select mapped from resource_map where cluster_id=$1 and res=$2 limit 1"
)

func parseResourceN(mappedResources []interface{}, selected int) (string, error) {

	ref := mappedResources[selected]

	switch ref.(type) {
	case string:
		return ref.(string), nil
	}

	return "", errors.New("unable to parse ref: " + strconv.Itoa(selected))
}

func selectResourceFromArray(clusterId int, mappedResources []interface{}) (string, error) {
	sz := len(mappedResources)
	if sz == 0 {
		return "", errors.New("MappedResources array should not be empty")
	}

	//TODO: Implement Octo Fox multi armed bandit selection function

	selected := rand.Intn(sz)

	return parseResourceN(mappedResources, selected)
}

func SelectResource(clusterId int, encodedMappedResources string) (string, error) {
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
		return selectResourceFromArray(clusterId, mappedResources.([]interface{}))
	}
	return "", errors.New("unable to map mappedResources json to a valid type: " + encodedMappedResources)
}

func GetMappedResources(db *sql.DB, clusterId int, res string) (string, error) {

	var mappedResources string

	stmt, err := db.Prepare(sqlGetMappedResources)
	if err != nil {
		return mappedResources, err
	}

	row := stmt.QueryRow(clusterId, res)
	if err := row.Scan(&mappedResources); err != nil {
		return mappedResources, err
	}

	return mappedResources, nil
}
