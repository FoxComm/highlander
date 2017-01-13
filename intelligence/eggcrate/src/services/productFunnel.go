package services

import (
	"encoding/json"
	"net/http"
	"responses"
	"strings"
	"util"

	"os"

	"fmt"

	"github.com/labstack/echo"
)

var url = os.Getenv("API_URL")
var port = os.Getenv("HENHOUSE_PORT")

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
	if port == "" {
		var portErr error
		_, port, portErr = util.LookupSrv("henhouse.service.consul")
		if portErr != nil {
			return "", portErr
		}
	}

	var key = ""
	steps := [...]string{"list", "pdp", "cart", "checkout"}
	for _, step := range steps {
		key += "track_product_" + id + "_" + step + ","
	}

	if a != "" {
		key += "&a=" + a
	}
	if b != "" {
		key += "&b=" + b
	}

	fmt.Println("requesting diff with keys=", key)
	resp, reqErr := http.Get(url + ":" + port + "/diff?keys=" + key)
	if reqErr != nil {
		return "", reqErr
	}

	var pf responses.HenhouseResponse
	err := json.NewDecoder(resp.Body).Decode(&pf)
	if err != nil {
		return "", err
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
		CartToCheckout: cartToCheckout}
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
