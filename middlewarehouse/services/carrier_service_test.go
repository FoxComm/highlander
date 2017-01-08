package services

import (
	"fmt"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/stretchr/testify/suite"
)

type CarrierServiceTestSuite struct {
	GeneralServiceTestSuite
	service ICarrierService
}

func TestCarrierServiceSuite(t *testing.T) {
	suite.Run(t, new(CarrierServiceTestSuite))
}

func (suite *CarrierServiceTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	repository := repositories.NewCarrierRepository(suite.db)
	suite.service = NewCarrierService(repository)
}

func (suite *CarrierServiceTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"carriers",
	})
}

func (suite *CarrierServiceTestSuite) Test_GetCarriers_ReturnsCarrierModels() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	carrier2 := fixtures.GetCarrier(uint(2))
	suite.Nil(suite.db.Create(carrier1).Error)
	suite.Nil(suite.db.Create(carrier2).Error)

	//act
	carriers, err := suite.service.GetCarriers()

	//assert
	suite.Nil(err)

	suite.Equal(2, len(carriers))
	suite.Equal(carrier1, carriers[0])
	suite.Equal(carrier2, carriers[1])
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierById_NotFound_ReturnsNotFoundError() {
	//act
	_, err := suite.service.GetCarrierByID(uint(1))

	//assert
	suite.Equal(fmt.Errorf(repositories.ErrorCarrierNotFound, 1), err)
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierByID_Found_ReturnsCarrierModel() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.Nil(suite.db.Create(carrier1).Error)

	//act
	carrier, err := suite.service.GetCarrierByID(uint(1))

	//assert
	suite.Nil(err)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierServiceTestSuite) Test_CreateCarrier_ReturnsCreatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))

	//act
	carrier, err := suite.service.CreateCarrier(carrier1)

	//assert
	suite.Nil(err)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))

	//act
	_, err := suite.service.UpdateCarrier(carrier1)

	//assert
	suite.Equal(fmt.Errorf(repositories.ErrorCarrierNotFound, 1), err)
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier_Found_ReturnsUpdatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.Nil(suite.db.Create(carrier1).Error)
	carrier1.Name = "Updated"

	//act
	carrier, err := suite.service.UpdateCarrier(carrier1)

	//assert
	suite.Nil(err)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
	//act
	err := suite.service.DeleteCarrier(uint(1))

	//assert
	suite.Equal(fmt.Errorf(repositories.ErrorCarrierNotFound, 1), err)
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier_Found_ReturnsNoError() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.Nil(suite.db.Create(carrier1).Error)

	//act
	err := suite.service.DeleteCarrier(uint(1))

	//assert
	suite.Nil(err)
}
