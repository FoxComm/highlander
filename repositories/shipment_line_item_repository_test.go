package repositories

import (
	"database/sql/driver"
	"testing"

	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"

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
	suite.db.Close()
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_GetShipmentLineItemsByShipmentID_Found_ReturnsShipmentLineItemModels() {
	//arrange
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	shipmentLineItem2 := suite.getTestShipmentLineItem1()
	rows := sqlmock.
		NewRows(suite.getShipmentLineItemColumns()).
		AddRow(suite.getShipmentLineItemRow(shipmentLineItem1)...).
		AddRow(suite.getShipmentLineItemRow(shipmentLineItem2)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipment_line_items" WHERE .+ \(\(shipment_id = \?\)\)`).
		WithArgs(uint(1)).
		WillReturnRows(rows)

	//act
	shipmentLineItems, err := suite.repository.GetShipmentLineItemsByShipmentID(uint(1))

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(2, len(shipmentLineItems))
	suite.assert.Equal(shipmentLineItem1, shipmentLineItems[0])
	suite.assert.Equal(shipmentLineItem2, shipmentLineItems[1])

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_CreateShipmentLineItem_ReturnsCreatedRecord() {
	//arrange
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	suite.mock.
		ExpectExec(`INSERT INTO "shipment_line_items"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows(suite.getShipmentLineItemColumns()).
		AddRow(suite.getShipmentLineItemRow(shipmentLineItem1)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipment_line_items" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shipmentLineItem, err := suite.repository.CreateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(uint(1), shipmentLineItem.ID)
	shipmentLineItem1.CreatedAt = shipmentLineItem.CreatedAt
	shipmentLineItem1.UpdatedAt = shipmentLineItem.UpdatedAt
	suite.assert.Equal(shipmentLineItem1, shipmentLineItem)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_UpdateShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	suite.mock.
		ExpectExec(`UPDATE "shipment_line_items"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentLineItemRepositoryTestSuite) Test_UpdateShipmentLineItem_Found_ReturnsUpdatedRecord() {
	//arrange
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	suite.mock.
		ExpectExec(`UPDATE "shipment_line_items"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows(suite.getShipmentLineItemColumns()).
		AddRow(suite.getShipmentLineItemRow(shipmentLineItem1)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipment_line_items" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shipmentLineItem, err := suite.repository.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Nil(err)
	shipmentLineItem1.CreatedAt = shipmentLineItem.CreatedAt
	shipmentLineItem1.UpdatedAt = shipmentLineItem.UpdatedAt
	suite.assert.Equal(shipmentLineItem1, shipmentLineItem)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
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

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
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

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentLineItemRepositoryTestSuite) getTestShipmentLineItem1() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(1)}, uint(1), "BR1002", "SKU-TEST1",
		"Some shit", 3999, "https://test.com/some-shit.png", "pending"}
}

func (suite *ShipmentLineItemRepositoryTestSuite) getTestShipmentLineItem2() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(2)}, uint(1), "BR1003", "SKU-TEST2",
		"Other shit", 4999, "https://test.com/other-shit.png", "delivered"}
}

func (suite *ShipmentLineItemRepositoryTestSuite) getShipmentLineItemColumns() []string {
	return []string{"id", "shipment_id", "name", "reference_number", "sku", "price",
		"image_path", "state", "created_at", "updated_at", "deleted_at"}
}

func (suite *ShipmentLineItemRepositoryTestSuite) getShipmentLineItemRow(shipmentLineItem *models.ShipmentLineItem) []driver.Value {
	return []driver.Value{shipmentLineItem.ID, shipmentLineItem.ShipmentID, shipmentLineItem.Name,
		shipmentLineItem.ReferenceNumber, shipmentLineItem.SKU, shipmentLineItem.Price, shipmentLineItem.ImagePath,
		shipmentLineItem.State, shipmentLineItem.CreatedAt, shipmentLineItem.UpdatedAt, shipmentLineItem.DeletedAt}
}
