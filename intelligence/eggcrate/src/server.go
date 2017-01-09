package main

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/labstack/echo"
)

// productFunnel.go
///////////////////
func getProductFunnel(c echo.Context) error {
	id := c.Param("id")
	resp, err := henhouseProductFunnel(id)
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}
	return c.String(http.StatusOK, resp)
}

func henhouseProductFunnel(id string) (string, error) {
	var pf HenhouseResponse
	// var key = "track_product_" + id + "_list"
	var key = ""
	steps := [...]string{"list", "pdp", "cart", "checkout"}
	for _, step := range steps {
		key += "track_product_" + id + "_" + step + ","
	}

	resp, reqErr := http.Get("http://hal.foxcommerce.com:31526/summary?keys=" + key)
	if reqErr != nil {
		return "", reqErr
	}

	err := json.NewDecoder(resp.Body).Decode(&pf)
	if err != nil {
		return "", err
	}

	return buildResponse(pf), nil
}

func buildResponse(pf HenhouseResponse) string {
	searchViews := getSum("list", pf)
	pdpViews := getSum("pdp", pf)
	cartClicks := getSum("cart", pf)
	checkoutClicks := getSum("checkout", pf)
	resp := ProductFunnelResponse{
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

func getSum(step string, pf HenhouseResponse) int {
	for i := range pf {
		if strings.Contains(pf[i].Key, step) {
			return pf[i].Stats.Sum
		}
	}
	return 0
}

type ProductFunnelResponse struct {
	SearchViews    int
	PdpViews       int
	CartClicks     int
	CheckoutClicks int
	SearchToPdp    float32
	PdpToCart      float32
	CartToCheckout float32
}

// henhouseResponse.go
//////////////////////
type HenhouseStats struct {
	Variance   float32 `json:"variance"`
	Mean       float32 `json:"mean"`
	From       int     `json:"from"`
	To         int     `json:"to"`
	Resolution int     `json:"resolution"`
	Points     int     `json:"points"`
	Sum        int     `json:"sum"`
}

type HenhouseResponse []struct {
	Key   string        `json:"key"`
	Stats HenhouseStats `json:"stats"`
}

// server.go
////////////
func main() {
	e := echo.New()
	e.GET("/productFunnel/:id", getProductFunnel)
	e.Logger.Fatal(e.Start(":1323"))
}
