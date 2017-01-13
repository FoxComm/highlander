package main

import (
	"services"

	"net/http"
	"os"

	"github.com/labstack/echo"
)

func main() {
	PORT := ":" + os.Getenv("PORT")
	e := echo.New()
	e.GET("/ping", func(c echo.Context) error {
		return c.String(http.StatusOK, "pong")
	})
	e.GET("/productFunnel/:id", services.GetProductFunnel)
	e.Logger.Fatal(e.Start(PORT))
}
