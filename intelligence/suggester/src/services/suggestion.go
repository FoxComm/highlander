package services

import (
	"encoding/json"
	"net/http"

	"github.com/FoxComm/highlander/intelligence/suggester/src/util"
	"github.com/labstack/echo"
)

func GetSuggestion(c echo.Context) error {
	customerId := c.Param("id")
	channel := c.QueryParam("channel")

	queryResponse, queryError := util.AntHillQuery(customerId, channel)
	if queryError != nil {
		return c.String(http.StatusBadRequest, queryError.Error())
	}

	decodedResponse, _ := json.Marshal(&queryResponse)
	return c.String(http.StatusOK, string(decodedResponse))
}
