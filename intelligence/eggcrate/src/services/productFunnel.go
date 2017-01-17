package services

import (
	"encoding/json"
	"net/http"
	"responses"
	"strings"
	"util"

	"github.com/labstack/echo"
)

func GetProductFunnel(c echo.Context) error {
	id := c.Param("id")
	if id != "" {
		id += "_"
	}
	from, to := c.QueryParam("from"), c.QueryParam("to")
	resp, err := henhouseProductFunnel(id, from, to)
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}
	return c.String(http.StatusOK, resp)
}

func henhouseProductFunnel(id, a, b string) (string, error) {
	steps := []string{"list", "pdp", "cart", "checkout"}
	pf, qErr := util.ProductQuery(id, steps, a, b)
	if qErr != nil {
		return "", qErr
	}

	return buildResponse(pf), nil
}

func buildResponse(pf responses.HenhouseResponse) string {
	searchViews := getSum("list", pf)
	pdpViews := getSum("pdp", pf)
	cartClicks := getSum("cart", pf)
	checkoutClicks := getSum("checkout", pf)
	purchases := getSum("purchase", pf)
	var searchToPdp, pdpToCart, cartToCheckout, checkoutPurchased float32
	if searchToPdp = 0.0; searchViews > 0.0 {
		searchToPdp = float32(pdpViews) / float32(searchViews)
	}
	if pdpToCart = 0.0; pdpViews > 0.0 {
		pdpToCart = float32(cartClicks) / float32(pdpViews)
	}
	if cartToCheckout = 0.0; cartClicks > 0.0 {
		cartToCheckout = float32(checkoutClicks) / float32(cartClicks)
	}
	if checkoutPurchased = 0.0; checkoutClicks > 0.0 {
		checkoutPurchased = float32(purchases) / float32(checkoutClicks)
	}

	resp := responses.ProductFunnelResponse{
		SearchViews:       searchViews,
		PdpViews:          pdpViews,
		CartClicks:        cartClicks,
		CheckoutClicks:    checkoutClicks,
		Purchases:         purchases,
		SearchToPdp:       searchToPdp,
		PdpToCart:         pdpToCart,
		CartToCheckout:    cartToCheckout,
		CheckoutPurchased: checkoutPurchased,
	}
	out, _ := json.Marshal(&resp)
	return string(out)
}

func getSum(step string, pf responses.HenhouseResponse) int {
	for i := range pf {
		if strings.Contains(pf[i].Key, step) {
			return pf[i].Stats.Sum
		}
	}
	return 0
}
