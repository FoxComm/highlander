package main

import (
	"encoding/json"
	"net/http"
	"strconv"

	"fmt"

	"github.com/labstack/echo"
)

func getTest(c echo.Context) error {
	res, _ := http.Get("https://httpbin.org/get")
	var body struct {
		// httpbin.org sends back key/value pairs, no map[string][]string
		Headers map[string]string `json:"headers"`
		Origin  string            `json:"origin"`
	}
	json.NewDecoder(res.Body).Decode(&body)
	fmt.Println(body)
	return c.String(http.StatusOK, body.Origin)
}

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
	var pf henhouseResponse
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
	var output = ""
	for i := range pf {
		output += pf[i].Key + " has sum = " + strconv.Itoa(pf[i].Stats.Sum) + "\n"
	}
	return output, nil
}

// henhouseResponse.go
//////////////////////
type henhouseStats struct {
	Variance   float32 `json:"variance"`
	Mean       float32 `json:"mean"`
	From       int     `json:"from"`
	To         int     `json:"to"`
	Resolution int     `json:"resolution"`
	Points     int     `json:"points"`
	Sum        int     `json:"sum"`
}

type henhouseResponse []struct {
	Key   string        `json:"key"`
	Stats henhouseStats `json:"stats"`
}

// server.go
////////////
func main() {
	e := echo.New()
	/*
		e.GET("/", func(c echo.Context) error {
			return c.String(http.StatusOK, "Hello, world!")
		})
	*/
	e.GET("/", getTest)

	e.GET("/productFunnel/:id", getProductFunnel)
	e.Logger.Fatal(e.Start(":1323"))
}
