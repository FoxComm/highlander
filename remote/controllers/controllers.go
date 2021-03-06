package controllers

import (
	"log"

	"github.com/FoxComm/highlander/remote/payloads"
	"github.com/FoxComm/highlander/remote/services"
	"github.com/FoxComm/highlander/remote/utils"
)

// Start initializes all the controllers and routes.
func Start() {
	config, err := utils.NewConfig()
	if err != nil {
		log.Fatal(err)
	}

	phxDB, err := services.NewPhoenixConnection(config)
	if err != nil {
		log.Fatal(err)
	}

	defer phxDB.Close()
	channelsCtrl := NewChannels(phxDB)

	r := NewRouter()

	r.GET("/v1/public/channels/:id", func(fc *FoxContext) error {
		id := fc.ParamInt("id")
		return fc.Run(channelsCtrl.GetChannel(id))
	})

	r.POST("/v1/public/channels", func(fc *FoxContext) error {
		payload := payloads.CreateChannel{}
		fc.BindJSON(&payload)
		return fc.Run(channelsCtrl.CreateChannel(&payload))
	})

	r.Run(config.Port)
}
