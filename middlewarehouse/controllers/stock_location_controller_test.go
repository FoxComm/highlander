package controllers

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type stockLocationControllerTestSuite struct {
	GeneralControllerTestSuite
	db      *gorm.DB
	service services.StockLocationService
}

func TestStockLocationControllerSuite(t *testing.T) {
	suite.Run(t, new(stockLocationControllerTestSuite))
}

func (suite *stockLocationControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()

	suite.service = services.NewStockLocationService(suite.db)

	controller := NewStockLocationController(suite.service)
	controller.SetUp(suite.router.Group("/stock-locations"))
}

func (suite *stockLocationControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{"stock_locations"})
}

func (suite *stockLocationControllerTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *stockLocationControllerTestSuite) Test_GetLocations() {
	models := []*models.StockLocation{
		{Name: "Location Name 1", Type: "Warehouse"},
		{Name: "Location Name 2", Type: "Warehouse"},
	}
	suite.Nil(suite.db.Create(models[0]).Error)
	suite.Nil(suite.db.Create(models[1]).Error)

	result := []responses.StockLocation{}
	response := suite.Get("/stock-locations", &result)

	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(2, len(result))
	suite.Equal(models[0].Name, result[0].Name)
	suite.Equal(models[1].Name, result[1].Name)
}

func (suite *stockLocationControllerTestSuite) Test_GetLocations_Empty() {
	result := []responses.StockLocation{}
	response := suite.Get("/stock-locations", &result)

	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(0, len(result))
}

func (suite *stockLocationControllerTestSuite) Test_GetLocationByID() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.Nil(suite.db.Create(model).Error)

	result := responses.StockLocation{}
	url := fmt.Sprintf("/stock-locations/%d", model.ID)
	response := suite.Get(url, &result)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(model.Name, result.Name)
}

func (suite *stockLocationControllerTestSuite) Test_GetLocationByID_NotFound() {
	response := suite.Get("/stock-locations/1")
	suite.Equal(http.StatusNotFound, response.Code)
}

func (suite *stockLocationControllerTestSuite) Test_CreateLocation() {
	model := fixtures.GetStockLocation()

	result := &responses.StockLocation{}
	jsonStr := fmt.Sprintf(`{"name":"%s","type":"%s"}`, model.Name, model.Type)
	response := suite.Post("/stock-locations", jsonStr, &result)

	suite.Equal(http.StatusCreated, response.Code)
	suite.Equal(model.Name, result.Name)
}

func (suite *stockLocationControllerTestSuite) Test_CreateLocation_Error() {
	jsonStr := `{"name":"Some warehouse","type":"wherehouse"}`
	response := suite.Post("/stock-locations", jsonStr)
	suite.Equal(http.StatusBadRequest, response.Code)
}

func (suite *stockLocationControllerTestSuite) Test_UpdateLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.Nil(suite.db.Create(model).Error)

	result := &responses.StockLocation{}
	jsonStr := fmt.Sprintf(`{"name":"%s","type":"%s"}`, "New Name", model.Type)
	url := fmt.Sprintf("/stock-locations/%d", model.ID)
	response := suite.Put(url, jsonStr, &result)

	suite.Equal(http.StatusOK, response.Code)
	suite.Equal("New Name", result.Name)
}

func (suite *stockLocationControllerTestSuite) Test_UpdateLocation_NotFound() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}

	jsonStr := fmt.Sprintf(`{"name":"%s","type":"%s"}`, model.Name, model.Type)
	response := suite.Put("/stock-locations/0", jsonStr)

	suite.Equal(http.StatusNotFound, response.Code)
}

func (suite *stockLocationControllerTestSuite) Test_DeleteLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.Nil(suite.db.Create(model).Error)

	response := suite.Delete("/stock-locations/1")

	suite.Equal(http.StatusNoContent, response.Code)
}

func (suite *stockLocationControllerTestSuite) Test_DeleteLocation_NotFound() {
	response := suite.Delete("/stock-locations/1")
	suite.Equal(http.StatusNotFound, response.Code)
}
