package service

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"log"

	"github.com/FoxComm/highlander/integrations/product-search/elastic"
	"github.com/FoxComm/highlander/middlewarehouse/consumers"
)

const (
	productSearch     = "api/search/admin/products_search_view/_search"
	productUpdateBase = "api/v1/products"
)

var (
	unexpectedError = ServiceError{500, errors.New("Unexpected error")}
)

type Client struct {
	elasticURL string
	phoenixURL string
	jwt        string
}

func NewClient(elasticURL, phoenixURL, jwt string) *Client {
	return &Client{elasticURL, phoenixURL, jwt}
}

func (c *Client) FindProductID(context, field, value string) (int, *ServiceError) {
	query := createQueryFilter(context, field, value)
	url := fmt.Sprintf("%s/%s", c.elasticURL, productSearch)
	headers := map[string]string{"JWT": c.jwt}

	resp, err := consumers.Post(url, headers, &query)
	if err != nil {
		se := &ServiceError{500, errors.New("Unexpected error querying for products")}
		return 0, se
	}

	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		se := &ServiceError{resp.StatusCode, errors.New("Unable to find product")}
		return 0, se
	}

	result, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading the product with %s", err.Error())
		return 0, &unexpectedError
	}

	esResult := new(elastic.ElasticResult)
	if err := json.Unmarshal(result, esResult); err != nil {
		log.Printf("Error decoding the product with %s", err.Error())
		return 0, &unexpectedError
	}

	if esResult.Pagination.Total == 0 {
		return 0, nil
	}

	if esResult.Pagination.Total > 1 {
		msg := fmt.Errorf(
			"Error selecting product - expected 1 found %d",
			esResult.Pagination.Total,
		)
		se := &ServiceError{400, msg}
		return 0, se
	}

	esProducts := new(elastic.ElasticProduct)
	if err := json.Unmarshal(result, esProducts); err != nil {
		log.Printf("Error decoding the product with %s", err.Error())
		return 0, &unexpectedError
	}

	productID := esProducts.Result[0].ProductID
	return productID, nil
}

func (c *Client) CreateProduct(context string, payload map[string]interface{}) (int, map[string]interface{}, *ServiceError) {
	url := fmt.Sprintf("%s/%s/%s", c.phoenixURL, productUpdateBase, context)
	headers := map[string]string{"JWT": c.jwt}

	resp, err := consumers.Post(url, headers, payload)
	if err != nil {
		return 0, nil, &ServiceError{500, err}
	}

	defer resp.Body.Close()
	result, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading the product create with %s", err.Error())
		return 0, nil, &unexpectedError
	}

	resJSON := make(map[string]interface{})
	if err := json.Unmarshal(result, &resJSON); err != nil {
		log.Printf("Error reading the product create response with %s", err.Error())
		return 0, nil, &unexpectedError
	}

	return resp.StatusCode, resJSON, nil
}

func (c *Client) UpdateProduct(context string, productID int, payload map[string]interface{}) (int, map[string]interface{}, *ServiceError) {
	url := fmt.Sprintf("%s/%s/%s/%v", c.phoenixURL, productUpdateBase, context, productID)
	headers := map[string]string{"JWT": c.jwt}

	resp, err := consumers.Patch(url, headers, payload)
	if err != nil {
		return 0, nil, &ServiceError{500, err}
	}

	defer resp.Body.Close()
	result, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading the product update with %s", err.Error())
		return 0, nil, &unexpectedError
	}

	resJSON := make(map[string]interface{})
	if err := json.Unmarshal(result, &resJSON); err != nil {
		log.Printf("Error reading the product response with %s", err.Error())
		return 0, nil, &unexpectedError
	}

	return resp.StatusCode, resJSON, nil
}

func createQueryFilter(context, field, value string) map[string]interface{} {
	return map[string]interface{}{
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"filter": []map[string]interface{}{
					map[string]interface{}{
						"missing": map[string]string{
							"field": "archivedAt",
						},
					},
					map[string]interface{}{
						"term": map[string]string{
							field: value,
						},
					},
					map[string]interface{}{
						"term": map[string]string{
							"context": context,
						},
					},
				},
			},
		},
	}
}
