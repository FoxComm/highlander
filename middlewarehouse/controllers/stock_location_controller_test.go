package controllers

// import (
// 	"net/http"
// 	"testing"

// 	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
// 	"github.com/FoxComm/highlander/middlewarehouse/controllers/mocks"
// 	"github.com/FoxComm/highlander/middlewarehouse/models"
// 	"github.com/FoxComm/highlander/middlewarehouse/fixtures"

// 	"errors"
// 	"fmt"

// 	"github.com/gin-gonic/gin"
// 	"github.com/jinzhu/gorm"
// 	"github.com/stretchr/testify/mock"
// 	"github.com/stretchr/testify/suite"
// )

// type stockLocationControllerTestSuite struct {
// 	GeneralControllerTestSuite
// 	service *mocks.StockLocationServiceMock
// }

// func TestStockLocationControllerSuite(t *testing.T) {
// 	suite.Run(t, new(stockLocationControllerTestSuite))
// }

// func (suite *stockLocationControllerTestSuite) SetupSuite() {
// 	suite.router = gin.New()

// 	suite.service = &mocks.StockLocationServiceMock{}

// 	controller := NewStockLocationController(suite.service)
// 	controller.SetUp(suite.router.Group("/stock-locations"))
// }

// func (suite *stockLocationControllerTestSuite) TearDownTest() {
// 	// clear service mock calls expectations after each test
// 	suite.service.ExpectedCalls = []*mock.Call{}
// 	suite.service.Calls = []mock.Call{}
// }

// func (suite *stockLocationControllerTestSuite) Test_GetLocations() {
// 	models := []*models.StockLocation{
// 		{Name: "Location Name 1", Type: "Warehouse"},
// 		{Name: "Location Name 2", Type: "Warehouse"},
// 	}
// 	suite.service.On("GetLocations").Return(models, nil).Once()

// 	result := []responses.StockLocation{}
// 	response := suite.Get("/stock-locations", &result)

// 	suite.Equal(http.StatusOK, response.Code)
// 	suite.Equal(2, len(result))
// 	suite.Equal(models[0].Name, result[0].Name)
// 	suite.Equal(models[1].Name, result[1].Name)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_GetLocations_Empty() {
// 	suite.service.On("GetLocations").Return([]*models.StockLocation{}, nil).Once()

// 	result := []responses.StockLocation{}
// 	response := suite.Get("/stock-locations", &result)

// 	suite.Equal(http.StatusOK, response.Code)
// 	suite.Equal(0, len(result))
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_GetLocations_Error() {
// 	suite.service.On("GetLocations").Return(nil, errors.New("Error")).Once()

// 	response := suite.Get("/stock-locations")

// 	suite.Equal(http.StatusInternalServerError, response.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_GetLocationByID() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.service.On("GetLocationByID", uint(1)).Return(model, nil).Once()

// 	result := responses.StockLocation{}
// 	response := suite.Get("/stock-locations/1", &result)

// 	//assert
// 	suite.Equal(http.StatusOK, response.Code)
// 	suite.Equal(model.Name, result.Name)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_GetLocationByID_NotFound() {
// 	suite.service.On("GetLocationByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

// 	response := suite.Get("/stock-locations/1")

// 	suite.Equal(http.StatusNotFound, response.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_GetLocationByID_Error() {
// 	suite.service.On("GetLocationByID", uint(1)).Return(nil, errors.New("Error")).Once()

// 	response := suite.Get("/stock-locations/1")

// 	suite.Equal(http.StatusBadRequest, response.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_CreateLocation() {
// 	model := fixtures.GetStockLocation()
// 	suite.service.On("CreateLocation", model).Return(model, nil).Once()

// 	result := &responses.StockLocation{}
// 	jsonStr := fmt.Sprintf(`{"name":"%s","type":"%s"}`, model.Name, model.Type)
// 	response := suite.Post("/stock-locations", jsonStr, &result)

// 	suite.Equal(http.StatusCreated, response.Code)
// 	suite.Equal(model.Name, result.Name)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_CreateLocation_Error() {
// 	model := fixtures.GetStockLocation()
// 	suite.service.On("CreateLocation", model).Return(nil, errors.New("Error")).Once()

// 	jsonStr := fmt.Sprintf(`{"name":"%s","type":"%s"}`, model.Name, model.Type)
// 	response := suite.Post("/stock-locations", jsonStr)

// 	suite.Equal(http.StatusBadRequest, response.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_UpdateLocation() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.service.On("UpdateLocation", model).Return(model, nil).Once()

// 	result := &responses.StockLocation{}
// 	jsonStr := fmt.Sprintf(`{"name":"%s","type":"%s"}`, model.Name, model.Type)
// 	response := suite.Put("/stock-locations/0", jsonStr, &result)

// 	suite.Equal(http.StatusOK, response.Code)
// 	suite.Equal(model.Name, result.Name)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_UpdateLocation_NotFound() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.service.On("UpdateLocation", model).Return(nil, gorm.ErrRecordNotFound).Once()

// 	jsonStr := fmt.Sprintf(`{"name":"%s","type":"%s"}`, model.Name, model.Type)
// 	response := suite.Put("/stock-locations/0", jsonStr)

// 	suite.Equal(http.StatusNotFound, response.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_DeleteLocation() {
// 	suite.service.On("DeleteLocation", uint(1)).Return(nil).Once()

// 	response := suite.Delete("/stock-locations/1")

// 	suite.Equal(http.StatusNoContent, response.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *stockLocationControllerTestSuite) Test_DeleteLocation_NotFound() {
// 	suite.service.On("DeleteLocation", uint(1)).Return(gorm.ErrRecordNotFound).Once()

// 	response := suite.Delete("/stock-locations/1")

// 	suite.Equal(http.StatusNotFound, response.Code)
// 	suite.service.AssertExpectations(suite.T())
// }
