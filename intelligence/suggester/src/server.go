package main

import (
	"encoding/json"
	"net/http"
	"os"

	"github.com/FoxComm/highlander/intelligence/suggester/src/services"
	"github.com/labstack/echo"
)

func main() {

	PORT := ":" + os.Getenv("PORT")

	e := echo.New()

	e.GET("/ping", func(c echo.Context) error {
		pongResponseMap := map[string]string{"message": "pong"}
		pongResponseJSON, _ := json.Marshal(pongResponseMap)
		return c.String(http.StatusOK, string(pongResponseJSON))
	})

	e.GET("/customer/:id", services.GetSuggestion)
	e.POST("/customer/:phone/decline", services.DeclineSuggestion)
	e.POST("/customer/:phone/purchase", services.PurchaseSuggestion)

	e.Logger.Fatal(e.Start(PORT))
}
