package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/cms/common"
	"github.com/gin-gonic/gin"
)

// NewContentRoutes updates the current routing engine with the routes for the
// content controller.
func NewContentRoutes(routePrefix string, router *gin.Engine) error {
	if err := validateRoutePrefix(routePrefix); err != nil {
		return err
	}

	group := router.Group(routePrefix)
	{
		group.POST("/:entity/:context", createHandler)
		group.GET("/:entity/:context/:id", readHandler)
		group.PATCH("/:entity/:context/:id", updateHandler)
		group.DELETE("/:entity/:context/:id", deleteHandler)
	}

	return nil
}

func createHandler() gin.HandlerFunc {
	return func(c *gin.Context) {
    entity, err := getParameter("entity")
    if err != nil {
      err.SetResponseError(c)
      return
    }

    context, err := getParameter("")
		c.JSON(http.StatusCreated, gin.H{"message": "created"})
	}
}

func readHandler() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"message": "read"})
	}
}

func updateHandler() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"message": "updated"})
	}
}

func deleteHandler() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"message": "deleted"})
	}
}

func getParameter(name string, c *gin.Context) (string, common.FoxError) {
	param := c.Param(name)
	if param == "" {
		return "", newParamIsRequiredError(name)
	}
	return param, nil
}


func create(c *gin.Context) (*response, common.FoxError) {
  entity, err :=
}
