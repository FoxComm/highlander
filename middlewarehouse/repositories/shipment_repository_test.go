package repositories

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/common/tests"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"fmt"

	"github.com/stretchr/testify/suite"
)

type ShipmentRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository     IShipmentRepository
	shippingMethod *models.ShippingMethod
	address        *models.Address
}

func TestShipmentRepositorySuite(t *testing.T) {
	suite.Run(t, new(ShipmentRepositoryTestSuite))
}

func (suite *ShipmentRepositoryTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	suite.repository = NewShipmentRepository(suite.db)

	tasks.TruncateTables(suite.db, []string{
		"carriers",
		"shipping_methods",
		"addresses",
	})

	carrier := fixtures.GetCarrier(1)
	suite.Nil(suite.db.Create(carrier).Error)

	suite.shippingMethod = fixtures.GetShippingMethod(1, carrier.ID, carrier)
	suite.Nil(suite.db.Create(suite.shippingMethod).Error)

	region := &models.Region{}
	suite.Nil(suite.db.Preload("Country").First(region).Error)
	suite.address = fixtures.GetAddress(1, region.ID, region)
	suite.Nil(suite.db.Create(suite.address).Error)
}

func (suite *ShipmentRepositoryTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"shipments",
	})
}

func (suite *ShipmentRepositoryTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *ShipmentRepositoryTestSuite) Test_GetShipmentsByID_Found_ReturnsShipmentModels() {
	//arrange
	shipment1 := fixtures.GetShipment(1, "BR10005", suite.shippingMethod.Code, suite.shippingMethod, suite.address.ID, suite.address, nil)
	shipment1.ReferenceNumber = "FS10002"
	suite.Nil(suite.db.Create(shipment1).Error)
	shipment2 := fixtures.GetShipment(2, "BR10005", suite.shippingMethod.Code, suite.shippingMethod, suite.address.ID, suite.address, nil)
	shipment2.ReferenceNumber = "FS10003"
	suite.Nil(suite.db.Create(shipment2).Error)

	//act
	shipments, err := suite.repository.GetShipmentsByOrder(shipment1.OrderRefNum)

	//assert
	suite.Nil(err)
	suite.Equal(2, len(shipments))
	tests.SyncDates(shipment1, shipment2, shipments[0], shipments[1],
		&shipment1.Address, &shipment2.Address, &shipments[0].Address, &shipments[1].Address)
	suite.Equal(shipment1, shipments[0])
	suite.Equal(shipment2, shipments[1])
}

func (suite *ShipmentRepositoryTestSuite) Test_GetShipmentByID_Found_ReturnsShipmentModel() {
	//arrange
	shipment1 := fixtures.GetShipment(1, "BR10005", suite.shippingMethod.Code, suite.shippingMethod, suite.address.ID, suite.address, []models.ShipmentLineItem{})
	suite.Nil(suite.db.Create(shipment1).Error)

	//act
	shipment, err := suite.repository.GetShipmentByID(shipment1.ID)

	//assert
	suite.Nil(err)
	tests.SyncDates(shipment1, shipment, &shipment1.Address, &shipment.Address)
	suite.Equal(shipment1, shipment)
}

func (suite *ShipmentRepositoryTestSuite) Test_CreateShipment_ReturnsCreatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipment(1, "BR10005", suite.shippingMethod.Code, suite.shippingMethod, suite.address.ID, suite.address, []models.ShipmentLineItem{})

	//act
	shipment, err := suite.repository.CreateShipment(fixtures.GetShipment(0, "BR10005", suite.shippingMethod.Code, suite.shippingMethod, suite.address.ID, suite.address, []models.ShipmentLineItem{}))

	//assert
	suite.Nil(err)
	tests.SyncDates(shipment1, shipment, &shipment1.Address, &shipment.Address)
	suite.Equal(shipment1, shipment)
}

func (suite *ShipmentRepositoryTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(1)

	//act
	_, err := suite.repository.UpdateShipment(shipment1)

	//assert
	suite.Equal(fmt.Errorf(ErrorShipmentNotFound, shipment1.ID).Error(), err.ToString())
}

func (suite *ShipmentRepositoryTestSuite) Test_UpdateShipment_Found_ReturnsUpdatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipment(1, "BR10005", suite.shippingMethod.Code, suite.shippingMethod, suite.address.ID, suite.address, []models.ShipmentLineItem{})
	suite.Nil(suite.db.Create(shipment1).Error)
	shipment1.State = models.ShipmentStateDelivered

	//act
	shipment, err := suite.repository.UpdateShipment(shipment1)

	//assert
	suite.Nil(err)
	tests.SyncDates(shipment1, shipment, &shipment1.Address, &shipment.Address)
	suite.Equal(shipment1, shipment)
}

func (suite *ShipmentRepositoryTestSuite) Test_DeleteShipment_NotFound_ReturnsNotFoundError() {
	//act
	err := suite.repository.DeleteShipment(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorShipmentNotFound, 1).Error(), err.ToString())
}

func (suite *ShipmentRepositoryTestSuite) Test_DeleteShipment_Found_ReturnsNoError() {
	//arrange
	shipment1 := fixtures.GetShipment(1, "BR10005", suite.shippingMethod.Code, suite.shippingMethod, suite.address.ID, suite.address, []models.ShipmentLineItem{})
	suite.Nil(suite.db.Create(shipment1).Error)

	//act
	err := suite.repository.DeleteShipment(shipment1.ID)

	//assert
	suite.Nil(err)
}
