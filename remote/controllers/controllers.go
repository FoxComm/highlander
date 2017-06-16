package controllers

import (
	"github.com/labstack/echo"
)

// Start initializes all the controllers and routes.
func Start() {
	e := echo.New()

	e.Use(func(h echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			fc := NewFoxContext(c)
			return h(fc)
		}
	})

	e.GET("/v1/public/channels/:id", func(c echo.Context) error {
		fc := c.(*FoxContext)
		id := fc.ParamInt("id")
		return fc.Run(GetChannel(id))
	})

	e.Start(":9898")
}
