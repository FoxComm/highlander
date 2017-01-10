package main

import (
	"github.com/FoxComm/highlander/intelligence/eggcrate/src/services"

	"net/http"
	"os"

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
	e.GET("/productFunnel/", services.GetProductFunnel)
	e.GET("/productSum/list/", services.GetProductSum("list"))
	e.GET("/productSum/pdp/", services.GetProductSum("pdp"))
	e.GET("/productSum/cart/", services.GetProductSum("cart"))
	e.GET("/productSum/checkout/", services.GetProductSum("checkout"))
	e.GET("/productFunnel/:id", services.GetProductFunnel)
	e.GET("/productSum/list/:id", services.GetProductSum("list"))
	e.GET("/productSum/pdp/:id", services.GetProductSum("pdp"))
	e.GET("/productSum/cart/:id", services.GetProductSum("cart"))
	e.GET("/productSum/checkout/:id", services.GetProductSum("checkout"))
	e.GET("/productStats/:channel/:id", services.GetProductStats)
	e.GET("/productFunnel/:id", services.GetProductFunnel)
	e.Logger.Fatal(e.Start(PORT))
}
