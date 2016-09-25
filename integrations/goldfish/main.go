package main

import (
	"encoding/json"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/gin-gonic/gin"
)

const searchQuery = `{"query":{"bool":{"filter":[{"missing":{"field":"archivedAt"}},{"term":{"externalId":"%s"}}]}}}`

type ProductQuery struct {
	Query string `json:"query"`
}

func createError(msg string) map[string][]string {
	return map[string][]string{
		"errors": []string{msg},
	}
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

		// var payload map[string]interface{}
		// if err := c.Bind(&payload); err != nil {
		// 	c.JSON(500, gin.H{
		// 		"errors": []string{"Unexpected error parsing payload"},
		// 	})
		// 	return
		// }

		// query := fmt.Sprintf(searchQuery, externalID)
		// query := make(map[string]interface{})

		query := map[string]interface{}{
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
								"externalId": externalID,
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

		// const searchQuery = `{"query":{"bool":{"filter":[{"missing":{"field":"archivedAt"}},{"term":{"externalId":"%s"}}]}}}`
		url := "https://admin-tgt-0916.foxcommerce.com/api/search/admin/products_search_view/_search?size=50"
		resp, err := consumers.Post(url, headers, &query)
		if err != nil {
			c.JSON(500, createError("Unexpected error querying for products"))
			return
		}

		defer resp.Body.Close()
		var payload map[string]interface{}

		if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
			msg := fmt.Sprintf("Error reading the product with %s", err.Error())
			c.JSON(500, createError(msg))
			return
		}

		if resp.StatusCode < 200 || resp.StatusCode > 299 {
			c.JSON(resp.StatusCode, payload)
			return
		}

		c.JSON(200, payload)
	})

	r.Run(":5492")
}
