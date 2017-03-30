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

	e.GET("/neo", services.TestNeo4j)

	// Decline or Purchase Endpoint
	// If purchase forward to the one-click
	// Else update the Neo4J with 'declined'
	//e.POST("/customer/:phone/purchase", services.PurchaseSku)
	//e.POST("/customer/:phone/decline", services.DeclineSku)

	e.Logger.Fatal(e.Start(PORT))
}
