package repositories

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type ShipmentRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IShipmentRepository
}

func TestShipmentRepositorySuite(t *testing.T) {
	suite.Run(t, new(ShipmentRepositoryTestSuite))
}

func (suite *ShipmentRepositoryTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()

	suite.repository = NewShipmentRepository(suite.db)
}

func (suite *ShipmentRepositoryTestSuite) TearDownTest() {
	//make sure that all expectations were met
	suite.Nil(suite.mock.ExpectationsWereMet())

	suite.db.Close()
}

func (suite *ShipmentRepositoryTestSuite) Test_GetShipmentsByReferenceNumber_Found_ReturnsShipmentModels() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	shipment2 := fixtures.GetShipmentShort(uint(2))
	shipmentRows := sqlmock.
		NewRows(fixtures.GetShipmentColumns()).
		AddRow(fixtures.GetShipmentRow(shipment1)...).
		AddRow(fixtures.GetShipmentRow(shipment2)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipments" WHERE .+ \(\(reference_number = \?\)\)`).
		WithArgs(shipment1.ReferenceNumber).
		WillReturnRows(shipmentRows)
	shippingMethodRows := sqlmock.
		NewRows(fixtures.GetShippingMethodColumns()).
		AddRow(fixtures.GetShippingMethodRow(&shipment1.ShippingMethod)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipping_methods" WHERE \("id" IN \(\?,\?\)\)`).
		WithArgs(shipment1.ShippingMethodID, shipment2.ShippingMethodID).
		WillReturnRows(shippingMethodRows)
	carrierRows := sqlmock.
		NewRows(fixtures.GetCarrierColumns()).
		AddRow(fixtures.GetCarrierRow(&shipment1.ShippingMethod.Carrier)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "carriers" WHERE \("id" IN \(\?,\?\)\)`).
		WithArgs(shipment1.ShippingMethod.CarrierID, shipment2.ShippingMethod.CarrierID).
		WillReturnRows(carrierRows)
	addressRows := sqlmock.
		NewRows(fixtures.GetAddressColumns()).
		AddRow(fixtures.GetAddressRow(&shipment1.Address)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "addresses" WHERE .+ \(\("id" IN \(\?,\?\)\)\)`).
		WithArgs(shipment1.AddressID, shipment2.AddressID).
		WillReturnRows(addressRows)
	regionRows := sqlmock.
		NewRows(fixtures.GetRegionColumns()).
		AddRow(fixtures.GetRegionRow(&shipment1.Address.Region)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "regions" WHERE \("id" IN \(\?,\?\)\)`).
		WithArgs(shipment1.Address.RegionID, shipment2.Address.RegionID).
		WillReturnRows(regionRows)
	countryRows := sqlmock.
		NewRows(fixtures.GetCountryColumns()).
		AddRow(fixtures.GetCountryRow(&shipment1.Address.Region.Country)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "countries" WHERE \("id" IN \(\?,\?\)\)`).
		WithArgs(shipment1.Address.Region.CountryID, shipment2.Address.Region.CountryID).
		WillReturnRows(countryRows)
	shipmentLineItemRows := sqlmock.
		NewRows(fixtures.GetShipmentLineItemColumns()).
		AddRow(fixtures.GetShipmentLineItemRow(&shipment1.ShipmentLineItems[0])...).
		AddRow(fixtures.GetShipmentLineItemRow(&shipment1.ShipmentLineItems[1])...).
		AddRow(fixtures.GetShipmentLineItemRow(&shipment2.ShipmentLineItems[0])...).
		AddRow(fixtures.GetShipmentLineItemRow(&shipment2.ShipmentLineItems[1])...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipment_line_items" WHERE .+ \(\("shipment_id" IN \(\?,\?\)\)\)`).
		WithArgs(shipment1.ID, shipment2.ID).
		WillReturnRows(shipmentLineItemRows)

	//act
	shipments, err := suite.repository.GetShipmentsByReferenceNumber(shipment1.ReferenceNumber)

	//assert
	suite.Nil(err)
	suite.Equal(2, len(shipments))
	suite.Equal(shipment1, shipments[0])
	//suite.Equal(shipment2, shipments[1])
}

func (suite *ShipmentRepositoryTestSuite) Test_GetShipmentByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows(fixtures.GetShipmentColumns())
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipments" WHERE .+ \(\("id" = \?\)\)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetShipmentByID(1)

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentRepositoryTestSuite) Test_GetShipmentByID_Found_ReturnsShipmentModel() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	suite.expectSelectByID(shipment1)

	//act
	shipment, err := suite.repository.GetShipmentByID(shipment1.ID)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1, shipment)
}

func (suite *ShipmentRepositoryTestSuite) Test_CreateShipment_ReturnsCreatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	suite.mock.
		ExpectExec(`INSERT INTO "shipments"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(shipment1)

	//act
	shipment, err := suite.repository.CreateShipment(fixtures.GetShipmentShort(uint(0)))

	//assert
	suite.Nil(err)
	shipment1.CreatedAt = shipment.CreatedAt
	shipment1.UpdatedAt = shipment.UpdatedAt
	suite.Equal(shipment1, shipment)
}

func (suite *ShipmentRepositoryTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	suite.mock.
		ExpectExec(`UPDATE "shipments"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateShipment(shipment1)

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentRepositoryTestSuite) Test_UpdateShipment_Found_ReturnsUpdatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	suite.mock.
		ExpectExec(`UPDATE "shipments"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(shipment1)

	//act
	shipment, err := suite.repository.UpdateShipment(shipment1)

	//assert
	suite.Nil(err)
	shipment1.CreatedAt = shipment.CreatedAt
	shipment1.UpdatedAt = shipment.UpdatedAt
	suite.Equal(shipment1, shipment)
}

func (suite *ShipmentRepositoryTestSuite) Test_DeleteShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "shipments" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	err := suite.repository.DeleteShipment(1)

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentRepositoryTestSuite) Test_DeleteShipment_Found_ReturnsNoError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "shipments" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.repository.DeleteShipment(1)

	//assert
	suite.Nil(err)
}

func (suite *ShipmentRepositoryTestSuite) expectSelectByID(shipment *models.Shipment) {
	shipmentRows := sqlmock.
		NewRows(fixtures.GetShipmentColumns()).
		AddRow(fixtures.GetShipmentRow(shipment)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipments" WHERE .+ \(\("id" = \?\)\)`).
		WithArgs(shipment.ID).
		WillReturnRows(shipmentRows)
	shippingMethodRows := sqlmock.
		NewRows(fixtures.GetShippingMethodColumns()).
		AddRow(fixtures.GetShippingMethodRow(&shipment.ShippingMethod)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipping_methods" WHERE \("id" IN \(\?\)\)`).
		WithArgs(shipment.ShippingMethodID).
		WillReturnRows(shippingMethodRows)
	carrierRows := sqlmock.
		NewRows(fixtures.GetCarrierColumns()).
		AddRow(fixtures.GetCarrierRow(&shipment.ShippingMethod.Carrier)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "carriers" WHERE \("id" IN \(\?\)\)`).
		WithArgs(shipment.ShippingMethod.CarrierID).
		WillReturnRows(carrierRows)
	addressRows := sqlmock.
		NewRows(fixtures.GetAddressColumns()).
		AddRow(fixtures.GetAddressRow(&shipment.Address)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "addresses" WHERE .+ \(\("id" IN \(\?\)\)\)`).
		WithArgs(shipment.AddressID).
		WillReturnRows(addressRows)
	regionRows := sqlmock.
		NewRows(fixtures.GetRegionColumns()).
		AddRow(fixtures.GetRegionRow(&shipment.Address.Region)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "regions" WHERE \("id" IN \(\?\)\)`).
		WithArgs(shipment.Address.RegionID).
		WillReturnRows(regionRows)
	countryRows := sqlmock.
		NewRows(fixtures.GetCountryColumns()).
		AddRow(fixtures.GetCountryRow(&shipment.Address.Region.Country)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "countries" WHERE \("id" IN \(\?\)\)`).
		WithArgs(shipment.Address.Region.CountryID).
		WillReturnRows(countryRows)
	shipmentLineItemRows := sqlmock.
		NewRows(fixtures.GetShipmentLineItemColumns()).
		AddRow(fixtures.GetShipmentLineItemRow(&shipment.ShipmentLineItems[0])...).
		AddRow(fixtures.GetShipmentLineItemRow(&shipment.ShipmentLineItems[1])...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipment_line_items" WHERE .+ \(\("shipment_id" IN \(\?\)\)\)`).
		WithArgs(shipment.ID).
		WillReturnRows(shipmentLineItemRows)
}
