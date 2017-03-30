package util

import (
	"fmt"
	"os"
	"strings"

	bolt "github.com/johnnadratowski/golang-neo4j-bolt-driver"
	"github.com/johnnadratowski/golang-neo4j-bolt-driver/structures/graph"
)

var neo4jUser = os.Getenv("NEO4J_USER")
var neo4jPass = os.Getenv("NEO4J_PASS")
var neo4jHost = os.Getenv("NEO4J_HOST")
var neo4jPort = os.Getenv("NEO4J_PORT")

func ConnectToNeo4J() (string, error) {
	driver := bolt.NewDriver()
	boltUrl := "bolt://" + neo4jUser + ":" + neo4jPass + "@" + neo4jHost + ":" + neo4jPort

	conn, connErr := driver.OpenNeo(boltUrl)
	if connErr != nil {
		return "Failed to connect to Neo4J", connErr
	}

	getAllNodesQuery := "START n=node(*) RETURN n"
	rowData, _, _, _ := conn.QueryNeoAll(getAllNodesQuery, nil)

	x := make([]string, len(rowData))
	for _, row := range rowData {
		rowString := fmt.Sprintf("NODE: %#v\n", row[0].(graph.Node)) // Prints all nodes
		x = append(x, rowString)
	}
	return strings.Join(x, ","), nil
}
