package services

import (
	"encoding/json"
	"net/http"

	"github.com/FoxComm/highlander/intelligence/eggcrate/src/responses"
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/util"

	"github.com/labstack/echo"
)

func GetProductStats(c echo.Context) error {
	id := c.Param("id")
	channel := c.Param("channel")

	from, to := c.QueryParam("from"), c.QueryParam("to")
	resp, err := henhouseProductStats(id, channel, from, to)
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}
	return c.String(http.StatusOK, resp)
}

func productStatKeys(id string, channel string) []string {
	all := "track." + channel + ".product."
	single := all + id + "."
	return []string{
		single + "revenue",
		single + "purchase",
		single + "purchase-quantity",
		single + "cart",
		single + "list",
		all + "revenue",
		all + "purchase",
		all + "purchase-quantity",
		all + "cart",
		all + "list",
		all + "active",
	}
}

func henhouseProductStats(id, channel, a, b string) (string, error) {
	keys := productStatKeys(id, channel)
	pf, qErr := util.HenhouseQuery(keys, a, b)
	if qErr != nil {
		return "", qErr
	}

	return buildStatResponse(pf), nil
}

func buildStatResponse(pf responses.HenhouseResponse) string {
	//get stats for the particular product
	revenue := responses.GetSum("revenue", pf)
	ordersWithProduct := responses.GetSum("purchase.", pf)
	ordered := responses.GetSum("purchase-quantity", pf)
	addedToCart := responses.GetSum("cart", pf)
	listed := responses.GetSum("list", pf)
	inCart := addedToCart - int(ordered)
	numberPerOrder := 0.0
	if ordersWithProduct > 0 {
		numberPerOrder = float64(ordered) / float64(ordersWithProduct)
	}

	conversionRate := 0.0
	if listed > 0 {
		conversionRate = float64(ordered) / float64(listed)
	}

	//get stats across all products
	allRevenue := responses.GetSum("product.revenue", pf)
	allOrdersWithProduct := responses.GetSum("product.purchase.", pf)
	allOrdered := responses.GetSum("product.purchase-quantity", pf)
	allAddedToCart := responses.GetSum("product.cart", pf)
	allListed := responses.GetSum("product.list", pf)
	allNumberPerOrder := 0.0
	if allOrdersWithProduct > 0 {
		allNumberPerOrder = float64(allOrdered) / float64(allOrdersWithProduct)
	}

	allInCart := allAddedToCart - allOrdered

	allConversionRate := 0.0
	if allListed > 0 {
		allConversionRate = float64(allOrdered) / float64(allListed)
	}

	avgRevenue := 0.0
	avgOrdered := 0.0
	avgInCart := 0.0
	avgNumberPerOrder := 0.0
	avgConversionRate := 0.0

	//stores the count of all products active
	activeProducts := float64(responses.GetSum("product.active", pf))

	if activeProducts > 0 {
		avgRevenue = float64(allRevenue) / activeProducts
		avgOrdered = float64(allOrdered) / activeProducts
		avgInCart = float64(allInCart) / activeProducts
		avgNumberPerOrder = allNumberPerOrder / activeProducts
		avgConversionRate = allConversionRate / activeProducts
	}

	resp := responses.ProductStatsResponse{
		TotalRevenue:                 revenue,
		TotalOrders:                  ordered,
		AverageNumberPerOrder:        numberPerOrder,
		TotalInCarts:                 inCart,
		ProductConversionRate:        conversionRate,
		AverageTotalRevenue:          avgRevenue,
		AverageTotalOrders:           avgOrdered,
		AverageAverageNumberPerOrder: avgNumberPerOrder,
		AverageTotalInCarts:          avgInCart,
		AverageProductConversionRate: avgConversionRate,
	}
	out, _ := json.Marshal(&resp)
	return string(out)
}
