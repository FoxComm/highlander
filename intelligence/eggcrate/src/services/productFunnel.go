package services

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/FoxComm/highlander/intelligence/eggcrate/src/responses"
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/util"

	"github.com/labstack/echo"
)

func GetProductFunnel(c echo.Context) error {
	id := c.Param("id")
	if id == "" {
		return c.String(http.StatusBadRequest, "id is required")
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
	var searchToPdp, pdpToCart, cartToCheckout float32
	if searchToPdp = 0.0; searchViews > 0.0 {
		searchToPdp = float32(pdpViews) / float32(searchViews)
	}
	if pdpToCart = 0.0; pdpViews > 0.0 {
		pdpToCart = float32(cartClicks) / float32(pdpViews)
	}
	if cartToCheckout = 0.0; cartClicks > 0.0 {
		cartToCheckout = float32(checkoutClicks) / float32(cartClicks)
	}

	resp := responses.ProductFunnelResponse{
		SearchViews:    searchViews,
		PdpViews:       pdpViews,
		CartClicks:     cartClicks,
		CheckoutClicks: checkoutClicks,
		SearchToPdp:    searchToPdp,
		PdpToCart:      pdpToCart,
		CartToCheckout: cartToCheckout,
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
