package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"

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

type ElasticProduct struct {
	Result []map[string]interface{} `json:"result"`
}

type ElasticResult struct {
	Pagination ElasticPagination `json:"pagination"`
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
		url := "https://tgt.foxcommerce.com/api/search/admin/products_search_view/_search?size=50"

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

		result, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			msg := fmt.Sprintf("Error reading the product with %s", err.Error())
			c.JSON(500, createError(msg))
			return
		}

		esResult := new(ElasticResult)
		if err := json.Unmarshal(result, esResult); err != nil {
			msg := fmt.Sprintf("Error decoding the product with %s", err.Error())
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

		esProducts := new(ElasticProduct)
		if err := json.Unmarshal(result, esProducts); err != nil {
			msg := fmt.Sprintf("Error decoding the product with %s", err.Error())
			c.JSON(500, createError(msg))
			return
		}

		productID, ok := esProducts.Result[0]["productId"]
		if !ok {
			c.JSON(500, createError("Unable to get productId from product"))
			return
		}

		payload := make(map[string]interface{})
		if err := c.BindJSON(&payload); err != nil {
			msg := fmt.Sprintf(
				"Unable to parse payload with error %s",
				err.Error(),
			)
			c.JSON(400, createError(msg))
			return
		}

		patchURL := fmt.Sprintf("https://tgt.foxcommerce.com/api/v1/products/%s/%v", context, productID)
		fmt.Printf("The URL is: %s", patchURL)
		patchResp, err := consumers.Patch(patchURL, headers, payload)
		if err != nil {
			msg := fmt.Sprintf(
				"Unable to update the product with error: %s",
				err.Error(),
			)
			c.JSON(500, createError(msg))
			return
		}

		defer patchResp.Body.Close()
		patchResult, err := ioutil.ReadAll(patchResp.Body)
		if err != nil {
			msg := fmt.Sprintf("Error reading the product update with %s", err.Error())
			c.JSON(500, createError(msg))
			return
		}

		patchJSON := make(map[string]interface{})
		if err := json.Unmarshal(patchResult, &patchJSON); err != nil {
			msg := fmt.Sprintf("Error weading the product response with %s", err.Error())
			c.JSON(500, createError(msg))
			return
		}

		c.JSON(patchResp.StatusCode, patchJSON)
	})

	r.Run(":5492")
}
