package main

import (
	"services"

	"github.com/labstack/echo"
)

func main() {
	e := echo.New()
	e.GET("/productFunnel/:id", services.GetProductFunnel)
	e.Logger.Fatal(e.Start(":1323"))
}
