package services

import (
	"net/http"

	"github.com/FoxComm/highlander/intelligence/suggester/src/util"

	"github.com/labstack/echo"
)

func GetSuggestion(c echo.Context) error {
	util.PingAntHill()
	return c.String(http.StatusOK, "")
}
