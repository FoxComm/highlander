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
		single + "cart",
		single + "list",
		single + "pdp",
		all + "revenue",
		all + "purchase",
		all + "cart",
		all + "list",
		all + "pdp",
	}
}

func henhouseProductStats(id, channel, a, b string) (string, error) {
	keys := productStatKeys(id, channel)
	pf, qErr := util.HenhouseQuery("diff", keys, a, b, "")
	if qErr != nil {
		return "", qErr
	}

	activeKey := []string{"track." + channel + ".product.active"}
	activeRes, qErr := util.HenhouseQuery("diff", activeKey, a, b, "agg")
	if qErr != nil {
		return "", qErr
	}

	activeProducts := responses.GetSum("product.active", activeRes)

	return buildStatResponse(pf, activeProducts), nil
}

func buildStatResponse(pf responses.HenhouseResponse, activeProducts int) string {
	//get stats for the particular product
	revenue := responses.GetSum("revenue", pf)
	ordered := responses.GetSum("purchase", pf)
	addedToCart := responses.GetSum("cart", pf)
	listed := responses.GetSum("list", pf)
	pdpViews := responses.GetSum("pdp", pf)
	inCart := addedToCart - int(ordered)

	conversionRate := 0.0
	if listed > 0 {
		conversionRate = float64(ordered) / float64(listed)
	}

	//get stats across all products
	allRevenue := responses.GetSum("product.revenue", pf)
	allOrdered := responses.GetSum("product.purchase", pf)
	allAddedToCart := responses.GetSum("product.cart", pf)
	allListed := responses.GetSum("product.list", pf)
	allPdpViews := responses.GetSum("product.pdp", pf)

	allInCart := allAddedToCart - allOrdered

	allConversionRate := 0.0
	if allListed > 0 {
		allConversionRate = float64(allOrdered) / float64(allListed)
	}

	avgRevenue := 0.0
	avgOrdered := 0.0
	avgInCart := 0.0
	avgPdpViews := 0.0
	avgConversionRate := 0.0

	//stores the count of all products active
	if activeProducts > 0 {
		flActive := float64(activeProducts)
		avgRevenue = float64(allRevenue) / flActive
		avgOrdered = float64(allOrdered) / flActive
		avgInCart = float64(allInCart) / flActive
		avgPdpViews = float64(allPdpViews) / flActive
		avgConversionRate = allConversionRate / flActive
	}

	average := &responses.AverageProductStatsResponse{
		TotalRevenue:          avgRevenue,
		TotalOrders:           avgOrdered,
		TotalPdPViews:         avgPdpViews,
		TotalInCarts:          avgInCart,
		ProductConversionRate: avgConversionRate,
	}

	resp := responses.ProductStatsResponse{
		TotalRevenue:          revenue,
		TotalOrders:           ordered,
		TotalPdPViews:         pdpViews,
		TotalInCarts:          inCart,
		ProductConversionRate: conversionRate,
		Average:               average,
		ActiveProducts:        activeProducts,
	}
	out, _ := json.Marshal(&resp)
	return string(out)
}
