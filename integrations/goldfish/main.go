package main

import (
	"encoding/json"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/gin-gonic/gin"
)

type ProductQuery struct {
	Query string `json:"query"`
}

func createError(msg string) map[string][]string {
	return map[string][]string{
		"errors": []string{msg},
	}
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

type Product map[string]interface{}

type ElasticResult struct {
	Pagination ElasticPagination
	Result     []Product
}

type ElasticPagination struct {
	Total int
}

func main() {
	r := gin.Default()
	r.POST("/products/external-id/:context/:id", func(c *gin.Context) {
		context := c.Param("context")
		externalID := c.Param("id")

		headers := map[string]string{
			"Content-Type": "application/json",
		}

		if values, _ := c.Request.Header["Jwt"]; len(values) == 1 {
			headers["JWT"] = values[0]
		} else {
			c.JSON(400, createError("JWT not found in request header"))
			return
		}

		query := createQueryFilter(context, "externalId", externalID)
		url := "https://admin-tgt-0916.foxcommerce.com/api/search/admin/products_search_view/_search?size=50"

		resp, err := consumers.Post(url, headers, &query)
		if err != nil {
			c.JSON(500, createError("Unexpected error querying for products"))
			return
		}

		defer resp.Body.Close()

		if resp.StatusCode < 200 || resp.StatusCode > 299 {
			var payload map[string]interface{}

			if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
				msg := fmt.Sprintf("Error reading the product with %s", err.Error())
				c.JSON(500, createError(msg))
				return
			}
			c.JSON(resp.StatusCode, payload)
			return
		}

		esResult := new(ElasticResult)
		if err := json.NewDecoder(resp.Body).Decode(esResult); err != nil {
			msg := fmt.Sprintf("Error reading the product with %s", err.Error())
			c.JSON(500, createError(msg))
			return
		}

		if esResult.Pagination.Total == 0 {
			c.JSON(404, createError("Product not found"))
			return
		}

		if esResult.Pagination.Total > 1 {
			msg := fmt.Sprintf(
				"Error selecting product - expected 1 found %d",
				esResult.Pagination.Total,
			)
			c.JSON(400, createError(msg))
			return
		}

		product := esResult.Result[0]
		productID, ok := product["productId"]
		if !ok {
			c.JSON(500, createError("Unable to get productId from product"))
			return
		}

		c.JSON(200, gin.H{"productID": productID})
	})

	r.Run(":5492")
}
