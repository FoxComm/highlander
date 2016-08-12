package repositories

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShipmentLineItemRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IShipmentLineItemRepository
}

func TestShipmentLineItemRepositorySuite(t *testing.T) {
	suite.Run(t, new(ShipmentLineItemRepositoryTestSuite))
}

func (suite *ShipmentLineItemRepositoryTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()

	suite.repository = NewShipmentLineItemRepository(suite.db)

	suite.assert = assert.New(suite.T())
}

func (suite *ShipmentLineItemRepositoryTestSuite) TearDownTest() {
	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())

	suite.db.Close()
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_GetShipmentLineItemsByShipmentID_Found_ReturnsShipmentLineItemModels() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	shipmentLineItem2 := fixtures.GetShipmentLineItem(uint(2), uint(1))
	rows := sqlmock.
		NewRows(fixtures.GetShipmentLineItemColumns()).
		AddRow(fixtures.GetShipmentLineItemRow(shipmentLineItem1)...).
		AddRow(fixtures.GetShipmentLineItemRow(shipmentLineItem2)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipment_line_items" WHERE .+ \(\(shipment_id = \?\)\)`).
		WithArgs(shipmentLineItem1.ShipmentID).
		WillReturnRows(rows)

	//act
	shipmentLineItems, err := suite.repository.GetShipmentLineItemsByShipmentID(shipmentLineItem1.ShipmentID)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(2, len(shipmentLineItems))
	suite.assert.Equal(shipmentLineItem1, shipmentLineItems[0])
	suite.assert.Equal(shipmentLineItem2, shipmentLineItems[1])
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_CreateShipmentLineItem_ReturnsCreatedRecord() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	suite.mock.
		ExpectExec(`INSERT INTO "shipment_line_items"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(shipmentLineItem1)

	//act
	shipmentLineItem, err := suite.repository.CreateShipmentLineItem(fixtures.GetShipmentLineItem(uint(0), uint(1)))

	//assert
	suite.assert.Nil(err)
	shipmentLineItem1.CreatedAt = shipmentLineItem.CreatedAt
	shipmentLineItem1.UpdatedAt = shipmentLineItem.UpdatedAt
	suite.assert.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_UpdateShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	suite.mock.
		ExpectExec(`UPDATE "shipment_line_items"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_UpdateShipmentLineItem_Found_ReturnsUpdatedRecord() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	suite.mock.
		ExpectExec(`UPDATE "shipment_line_items"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(shipmentLineItem1)

	//act
	shipmentLineItem, err := suite.repository.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Nil(err)
	shipmentLineItem1.CreatedAt = shipmentLineItem.CreatedAt
	shipmentLineItem1.UpdatedAt = shipmentLineItem.UpdatedAt
	suite.assert.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_DeleteShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "shipment_line_items" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	err := suite.repository.DeleteShipmentLineItem(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_DeleteShipmentLineItem_Found_ReturnsNoError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "shipment_line_items" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.repository.DeleteShipmentLineItem(1)

	//assert
	suite.assert.Nil(err)
}

func (suite *ShipmentLineItemRepositoryTestSuite) expectSelectByID(shipmentLineItem *models.ShipmentLineItem) {
	shipmentLineItemRows := sqlmock.
		NewRows(fixtures.GetShipmentLineItemColumns()).
		AddRow(fixtures.GetShipmentLineItemRow(shipmentLineItem)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipment_line_items" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(shipmentLineItem.ID).
		WillReturnRows(shipmentLineItemRows)
}
