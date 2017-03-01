package services

import (
	"encoding/json"
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/responses"
	"net/http"

	"github.com/labstack/echo"
)

func GetProductSum(step string) func(c echo.Context) error {
	return func(c echo.Context) error {
		id := c.Param("id")
		if id != "" {
			id += "_"
		}
		from, to := c.QueryParam("from"), c.QueryParam("to")
		resp, err := productSingleSum(id, step, from, to)
		if err != nil {
			return c.String(http.StatusBadRequest, err.Error())
		}
		return c.String(http.StatusOK, resp)
	}
}

func productSingleSum(id, step, from, to string) (string, error) {
	steps := []string{step}
	resp, err := ProductQuery(id, steps, from, to)
	if err != nil {
		return "", err
	}
	productSum := responses.ProductSumResponse{
		Step: step,
		Sum:  resp[0].Stats.Sum,
	}
	out, _ := json.Marshal(&productSum)
	return string(out), nil
}
