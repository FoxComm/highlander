package repositories

import (
	"fmt"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"

	"github.com/stretchr/testify/suite"
)

type CarrierRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository ICarrierRepository
}

func TestCarrierRepositorySuite(t *testing.T) {
	suite.Run(t, new(CarrierRepositoryTestSuite))
}

func (suite *CarrierRepositoryTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	suite.repository = NewCarrierRepository(suite.db)
}

func (suite *CarrierRepositoryTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"carriers",
	})
}

func (suite *CarrierRepositoryTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarriers_ReturnsCarrierModels() {
	//arrange
	carrier1 := fixtures.GetCarrier(1)
	suite.Nil(suite.db.Create(carrier1).Error)
	carrier2 := fixtures.GetCarrier(2)
	suite.Nil(suite.db.Create(carrier2).Error)

	//act
	carriers, err := suite.repository.GetCarriers()

	//assert
	suite.Nil(err)

	suite.Equal(2, len(carriers))
	suite.Equal(carrier1, carriers[0])
	suite.Equal(carrier2, carriers[1])
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarrierByID_NotFound_ReturnsNotFoundError() {
	//act
	_, err := suite.repository.GetCarrierByID(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorCarrierNotFound, 1).Error(), err.ToString())
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarrierByID_Found_ReturnsCarrierModel() {
	//arrange
	carrier1 := fixtures.GetCarrier(1)
	suite.Nil(suite.db.Create(carrier1).Error)

	//act
	carrier, err := suite.repository.GetCarrierByID(carrier1.ID)

	//assert
	suite.Nil(err)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierRepositoryTestSuite) Test_CreateCarrier_ReturnsCreatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(1)

	//act
	carrier, err := suite.repository.CreateCarrier(fixtures.GetCarrier(0))

	//assert
	suite.Nil(err)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierRepositoryTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	carrier1 := fixtures.GetCarrier(1)

	//act
	_, err := suite.repository.UpdateCarrier(carrier1)

	//assert
	suite.Equal(fmt.Errorf(ErrorCarrierNotFound, carrier1.ID).Error(), err.ToString())
}

func (suite *CarrierRepositoryTestSuite) Test_UpdateCarrier_Found_ReturnsUpdatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(1)
	suite.Nil(suite.db.Create(carrier1).Error)

	//act
	carrier, err := suite.repository.UpdateCarrier(carrier1)

	//assert
	suite.Nil(err)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierRepositoryTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
	//act
	err := suite.repository.DeleteCarrier(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorCarrierNotFound, 1).Error(), err.ToString())
}

func (suite *CarrierRepositoryTestSuite) Test_DeleteCarrier_Found_ReturnsNoError() {
	//arrange
	carrier1 := fixtures.GetCarrier(1)
	suite.Nil(suite.db.Create(carrier1).Error)

	//act
	err := suite.repository.DeleteCarrier(carrier1.ID)

	//assert
	suite.Nil(err)
}
