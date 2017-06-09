package routes

import (
    "github.com/FoxComm/highlander/search-service/golang/controllers"
    "github.com/FoxComm/highlander/search-service/golang/services"
)

func GetRoutes() map[string]controllers.IController {
    esService := services.NewElasticService()

    return map[string]controllers.IController{
        "/": controllers.NewSearchController(esService),
    }
}
