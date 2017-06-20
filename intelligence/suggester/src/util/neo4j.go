package util

import (
	"bytes"
	"crypto/sha1"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"strconv"

	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
)

var neo4jUser = os.Getenv("NEO4J_USER")
var neo4jPass = os.Getenv("NEO4J_PASS")
var neo4jHost = os.Getenv("NEO4J_HOST")
var neo4jPort = os.Getenv("NEO4J_PORT")

func hashPhoneNumber(phoneNumber string) string {
	sha1Hash := sha1.New()
	sha1Hash.Write([]byte(phoneNumber))
	hashBytes := sha1Hash.Sum(nil)
	phoneNumberHashString := fmt.Sprintf("%x", hashBytes)

	return phoneNumberHashString
}

func createNewSuggestProductCypher(customerID string, productID string, phoneNumberHash string, productSKU string) string {
	matchCustomer := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})", customerID)
	matchProduct := fmt.Sprintf("MATCH (p:Product {phoenix_id: %s})", productID)
	suggestRelation := fmt.Sprintf("MERGE (c)-[r:SUGGEST { phonehash: '%s' } ]->(p)", phoneNumberHash)
	addSKU := fmt.Sprintf("SET p.sku = '%s'", productSKU)
	returnRelation := "RETURN r"

	return matchCustomer + matchProduct + suggestRelation + addSKU + returnRelation
}

func createNewDeclinedProductCypher(customerID string, productID string) string {
	matchCustomer := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})", customerID)
	matchProduct := fmt.Sprintf("MATCH (p:Product {phoenix_id: %s})", productID)
	declinedRelation := "MERGE (c)-[r:DECLINED]->(p)"
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

func queryFindCustomerAndProductFromPhoneNumberHash(phoneNumberHash string) string {
	matchPhoneNumber := fmt.Sprintf("MATCH (c:Customer)-[r:SUGGEST {phonehash: '%s'}]->(p)", phoneNumberHash)
	whereNotPurchased := "WHERE NOT (c)-[:PURCHASED]->(p:Product)"
	andNotDeclined := "AND NOT (c)-[:DECLINED]->(p:Product)"
	returnCustomerProductRelation := "RETURN r"

	return matchPhoneNumber + whereNotPurchased + andNotDeclined + returnCustomerProductRelation
}

func deleteSuggestedProductRelationCypher(customerID string, productID string) string {
	match := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})-[r:SUGGEST]->(p:Product {phoenix_id: %s})", customerID, productID)
	deleteRelation := "DELETE r"

	return match + deleteRelation
}

func queryFindAllProductsSuggestedForCustomer(customerID string) string {
	matchCustomerSuggested := fmt.Sprintf("MATCH (c:Customer {phoenix_id: %s})-[r]->(p:Product)", customerID)
	wherePurchased := "WHERE (c)-[r:PURCHASED]->(p:Product)"
	whereDeclined := "OR (c)-[r:DECLINED]->(p:Product)"
	whereSuggested := "OR (c)-[r:SUGGEST]->(p:Product)"
	returnProducts := "RETURN p"

	return matchCustomerSuggested + wherePurchased + whereDeclined + whereSuggested + returnProducts
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

func neo4jPostRequest(requestPayload string) (responses.Neo4jResponse, error) {
	neo4jURL := "http://" + neo4jUser + ":" + neo4jPass + "@" + neo4jHost + ":" + neo4jPort + "/db/data/transaction/commit"
	var jsonStr = []byte(requestPayload)
	resp, postErr := http.Post(neo4jURL, "application/json", bytes.NewBuffer(jsonStr))
	if postErr != nil {
		return responses.Neo4jResponse{}, postErr
	}
	defer resp.Body.Close()

	var neo4jResponse responses.Neo4jResponse
	jsonErr := json.NewDecoder(resp.Body).Decode(&neo4jResponse)
	if jsonErr != nil {
		return responses.Neo4jResponse{}, jsonErr
	}

	return neo4jResponse, nil
}

func CreateNewSuggestProductRelation(customerID string, productID string, phoneNumber string, productSKU string) (responses.Neo4jResponse, error) {
	phoneNumberHash := hashPhoneNumber(phoneNumber)
	return neo4jPostRequest(makeRestPayload(createNewSuggestProductCypher(customerID, productID, phoneNumberHash, productSKU)))
}

func CreateNewDeclinedProductRelation(customerID string, productID string) (responses.Neo4jResponse, error) {
	return neo4jPostRequest(makeRestPayload(createNewDeclinedProductCypher(customerID, productID)))
}

func FindAllProductsSuggestedForCustomer(customerID string) (responses.Neo4jResponse, error) {
	return neo4jPostRequest(makeRestPayload(queryFindAllProductsSuggestedForCustomer(customerID)))
}

func FindCustomerAndProductFromPhoneNumber(phoneNumber string) (string, string, string, error) {
	hashedPhoneNumber := hashPhoneNumber(phoneNumber)
	requestPayload := makeRestPayload(queryFindCustomerAndProductFromPhoneNumberHash(hashedPhoneNumber))

	neo4jURL := "http://" + neo4jUser + ":" + neo4jPass + "@" + neo4jHost + ":" + neo4jPort + "/db/data/transaction/commit"
	var jsonStr = []byte(requestPayload)
	resp, err := http.Post(neo4jURL, "application/json", bytes.NewBuffer(jsonStr))
	if err != nil {
		return "", "", "", err
	}
	defer resp.Body.Close()

	var neo4jResponse responses.Neo4jResponse
	jsonErr := json.NewDecoder(resp.Body).Decode(&neo4jResponse)
	if jsonErr != nil {
		return "", "", "", jsonErr
	}

	var customerGraphNode responses.Neo4jResultsDataGraphNodes
	var productGraphNode responses.Neo4jResultsDataGraphNodes

	graphNodeLabels := neo4jResponse.Results[0].Data[0].Graph.Nodes
	for _, node := range graphNodeLabels {
		if node.Labels[0] == "Product" {
			productGraphNode = node
		}
		if node.Labels[0] == "Customer" {
			customerGraphNode = node
		}
	}

	customerID := strconv.Itoa(customerGraphNode.Properties.PhoenixID)
	productID := strconv.Itoa(productGraphNode.Properties.PhoenixID)
	productSKU := productGraphNode.Properties.SKU

	return customerID, productID, productSKU, nil
}
