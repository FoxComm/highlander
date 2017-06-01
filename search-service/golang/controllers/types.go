package controllers

import (
    "github.com/gin-gonic/gin"
)

type IController interface {
    SetUp(router gin.IRouter)
}
