package controllers

import (
	"log"

	"github.com/FoxComm/highlander/remote/db"
	"github.com/FoxComm/highlander/remote/utils"
	"github.com/labstack/echo"
)

// Start initializes all the controllers and routes.
func Start() {
	config, err := utils.NewConfig()
	if err != nil {
		log.Fatal(err)
	}

	phxDB, err := db.NewPhoenixConnection(config)
	if err != nil {
		log.Fatal(err)
	}

	defer phxDB.Close()
	channelsCtrl := NewChannels(phxDB)

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
		return fc.Run(channelsCtrl.GetChannel(id))
	})

	e.Start(":9898")
}
