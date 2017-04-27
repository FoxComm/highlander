package services

import (
	"encoding/json"
	"net/http"

	"github.com/FoxComm/highlander/intelligence/eggcrate/src/responses"
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/util"

	"github.com/labstack/echo"
)

func ProductQuery(id string, verbs []string, a, b string) (responses.HenhouseResponse, error) {
	var keys []string
	for _, verb := range verbs {
		keys = append(keys, "track_product_"+id+verb)
	}
	return util.HenhouseQuery("diff", keys, a, b, "")
}

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
	steps := []string{"list", "pdp", "cart", "checkout", "purchase"}
	pf, qErr := ProductQuery(id, steps, a, b)
	if qErr != nil {
		return "", qErr
	}

	return buildResponse(pf), nil
}

func buildResponse(pf responses.HenhouseResponse) string {
	searchViews := responses.GetSum("list", pf)
	pdpViews := responses.GetSum("pdp", pf)
	cartClicks := responses.GetSum("cart", pf)
	checkoutClicks := responses.GetSum("checkout", pf)
	purchases := responses.GetSum("purchase", pf)
	var searchToPdp, pdpToCart, cartToCheckout, checkoutPurchased float64
	if searchToPdp = 0.0; searchViews > 0.0 {
		searchToPdp = float64(pdpViews) / float64(searchViews)
	}
	if pdpToCart = 0.0; pdpViews > 0.0 {
		pdpToCart = float64(cartClicks) / float64(pdpViews)
	}
	if cartToCheckout = 0.0; cartClicks > 0.0 {
		cartToCheckout = float64(checkoutClicks) / float64(cartClicks)
	}
	if checkoutPurchased = 0.0; checkoutClicks > 0.0 {
		checkoutPurchased = float64(purchases) / float64(checkoutClicks)
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
