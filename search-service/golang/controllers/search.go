package controllers

import (
    "net/http"
    "fmt"
    "errors"
    "encoding/json"
    "strconv"

    "github.com/gin-gonic/gin"
    "github.com/SermoDigital/jose/jws"

    "github.com/FoxComm/highlander/search-service/golang/services"
)

type searchController struct {
    esService *services.ElasticService
}

func NewSearchController(esService *services.ElasticService) IController {
    return &searchController{ esService }
}

type body struct {
    Query json.RawMessage `json:"query"`
}

func (controller *searchController) SetUp(router gin.IRouter) {

    router.POST("/search/:index/:view", func(c *gin.Context) {

        index := c.Param("index")
        view := c.Param("view")
        from, _ := strconv.Atoi(c.DefaultQuery("from", "0"))
        size, _ := strconv.Atoi(c.DefaultQuery("size", "10"))

        var query body
        c.BindJSON(&query)

        fmt.Printf("index: %s, view: %s, from: %d, size: %d\n", index, view, from, size)

        response := controller.esService.GetEsResponse(index, view, from, size, query.Query)

        c.JSON(http.StatusOK, response)
    })
}


func FetchJWT(context *gin.Context) {
    rawJWT := context.Request.Header.Get("JWT")

    //override, if JWT given in cookie
    if cookieJWT, err := context.Request.Cookie("JWT"); err == nil {
        rawJWT = cookieJWT.Value
    }

    if rawJWT == "" {
        handleServiceError(context, errors.New("No JWT passed"))
        return
    }

    token, err := jws.ParseJWT([]byte(rawJWT))
    if err != nil {
        handleServiceError(context, fmt.Errorf("Token parse failure: %s", err.Error()))
        return
    }

    context.Keys = map[string]interface{}{
        "jwt": token,
    }

    context.Next()
}

func handleServiceError(context *gin.Context, err error) {
    fmt.Println("Error: failed to execute query: %+v", err)

    context.JSON(http.StatusBadRequest, err)
    context.Abort()
}
