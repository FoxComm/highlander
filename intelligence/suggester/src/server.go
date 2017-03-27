package main

import (
	"net/http"
	"os"

	"github.com/FoxComm/highlander/intelligence/suggester/src/services"
	"github.com/labstack/echo"
)

func main() {

	PORT := ":" + os.Getenv("PORT")

	e := echo.New()

	e.GET("/ping", func(c echo.Context) error {
		return c.String(http.StatusOK, "pong")
	})

	e.GET("/customer/:id", services.GetSuggestion)

	e.Logger.Fatal(e.Start(PORT))
}
