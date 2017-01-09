package services

import (
	"encoding/json"
	"net/http"
	"responses"
	"strings"

	"os"

	"github.com/labstack/echo"
)

var url = os.Getenv("URL")
var port = os.Getenv("PORT")

func GetProductFunnel(c echo.Context) error {
	id := c.Param("id")
	resp, err := henhouseProductFunnel(id)
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}
	return c.String(http.StatusOK, resp)
}

func henhouseProductFunnel(id string) (string, error) {
	var pf responses.HenhouseResponse
	var key = ""
	steps := [...]string{"list", "pdp", "cart", "checkout"}
	for _, step := range steps {
		key += "track_product_" + id + "_" + step + ","
	}

	// resp, reqErr := http.Get("http://hal.foxcommerce.com:31526/summary?keys=" + key)
	resp, reqErr := http.Get(url + ":" + port + "/summary?keys=" + key)
	if reqErr != nil {
		return "", reqErr
	}

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
	resp := responses.ProductFunnelResponse{
		SearchViews:    searchViews,
		PdpViews:       pdpViews,
		CartClicks:     cartClicks,
		CheckoutClicks: checkoutClicks,
		SearchToPdp:    float32(pdpViews) / float32(searchViews),
		PdpToCart:      float32(cartClicks) / float32(pdpViews),
		CartToCheckout: float32(checkoutClicks) / float32(cartClicks)}
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
