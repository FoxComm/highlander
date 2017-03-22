package services

import (
	"encoding/json"
	"net/http"

	"github.com/FoxComm/highlander/intelligence/suggester/src/util"

	"github.com/labstack/echo"
)

func GetSuggestion(c echo.Context) {
	customerId := c.Param("id")
	util.PingAntHill()
}
