package util

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
)

var neo4jUser = os.Getenv("NEO4J_USER")
var neo4jPass = os.Getenv("NEO4J_PASS")
var neo4jHost = os.Getenv("NEO4J_HOST")
var neo4jPort = os.Getenv("NEO4J_PORT")

func createNewSuggestProductCypher(customerID string, productID string) string {
	matchCustomer := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})", customerID)
	matchProduct := fmt.Sprintf("MATCH (p:Product {phoenix_id: %s})", productID)
	suggestRelation := "CREATE (c)-[r:SUGGEST]->(p)"
	returnRelation := "RETURN r"

	return matchCustomer + matchProduct + suggestRelation + returnRelation
}

func createNewDeclinedProductCypher(customerID string, productID string) string {
	matchCustomer := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})", customerID)
	matchProduct := fmt.Sprintf("MATCH (p:Product {phoenix_id: %s})", productID)
	declinedRelation := "CREATE (c)-[r:DECLINED]->(p)"
	returnRelation := "RETURN r"

	return matchCustomer + matchProduct + declinedRelation + returnRelation
}

func querySuggestedProductForPurchaseCypher(customerID string) string {
	matchCustomer := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})-[r:SUGGEST]->(p)", customerID)
	whereNotPurchased := "WHERE NOT (c)-[:PURCHASED]->(p:Product)"
	andNotDeclined := "AND NOT (c)-[:DECLINED]->(p:Product)"
	returnProduct := "RETURN p LIMIT 1"

	return matchCustomer + whereNotPurchased + andNotDeclined + returnProduct
}

func deleteSuggestedProductRelationCypher(customerID string, productID string) string {
	match := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})-[r:SUGGEST]->(p:Product {phoenix_id: %s})", customerID, productID)
	deleteRelation := "DELETE r"

	return match + deleteRelation
}

func makeRestPayload(statementBody string) string {
	return `{
		"statements": [
			{
				"statement": "` + statementBody + `",
				"parameters": null,
				"resultDataContents": [
					"row",
					"graph"
				],
				"includeStats": false
			}
		]
	}`
}

func CreateNewSuggestProductRelation(customerID string, productID string) (string, error) {
	return neo4jPostRequest(makeRestPayload(createNewSuggestProductCypher(customerID, productID)))
}

func CreateNewDeclinedProductRelation(customerID string, productID string) (string, error) {
	return neo4jPostRequest(makeRestPayload(createNewDeclinedProductCypher(customerID, productID)))
}

func neo4jPostRequest(requestPayload string) (string, error) {
	neo4jURL := "http://" + neo4jUser + ":" + neo4jPass + "@" + neo4jHost + ":" + neo4jPort + "/db/data/transaction/commit"
	var jsonStr = []byte(requestPayload)
	resp, err := http.Post(neo4jURL, "application/json", bytes.NewBuffer(jsonStr))
	if err != nil {
		return "request failed", err
	}
	defer resp.Body.Close()

	body, readErr := ioutil.ReadAll(resp.Body)
	if readErr != nil {
		return "parse response failed", readErr
	}

	return string(body), nil
}
