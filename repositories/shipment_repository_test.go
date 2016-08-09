package repositories

import (
	"database/sql"
	"database/sql/driver"
	"testing"

	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
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

	suite.assert = assert.New(suite.T())
}

func (suite *ShipmentRepositoryTestSuite) TearDownTest() {
	suite.db.Close()
}

func (suite *ShipmentRepositoryTestSuite) Test_GetShipmentsByReferenceNumber_Found_ReturnsShipmentModels() {
	//arrange
	shipment1 := suite.getTestShipment1()
	shipment2 := suite.getTestShipment1()
	rows := sqlmock.
		NewRows(suite.getShipmentColumns()).
		AddRow(suite.getShipmentRow(shipment1)...).
		AddRow(suite.getShipmentRow(shipment2)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipments" WHERE .+ \(\(reference_number = \?\)\)`).
		WithArgs("BR1005").
		WillReturnRows(rows)

	//act
	shipments, err := suite.repository.GetShipmentsByReferenceNumber("BR1005")

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(2, len(shipments))
	suite.assert.Equal(shipment1, shipments[0])
	suite.assert.Equal(shipment2, shipments[1])

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentRepositoryTestSuite) Test_CreateShipment_ReturnsCreatedRecord() {
	//arrange
	shipment1 := suite.getTestShipment1()
	suite.mock.
		ExpectExec(`INSERT INTO "shipments"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows(suite.getShipmentColumns()).
		AddRow(suite.getShipmentRow(shipment1)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipments" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shipment, err := suite.repository.CreateShipment(shipment1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(uint(1), shipment.ID)
	shipment1.CreatedAt = shipment.CreatedAt
	shipment1.UpdatedAt = shipment.UpdatedAt
	suite.assert.Equal(shipment1, shipment)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentRepositoryTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	shipment1 := suite.getTestShipment1()
	suite.mock.
		ExpectExec(`UPDATE "shipments"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateShipment(shipment1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentRepositoryTestSuite) Test_UpdateShipment_Found_ReturnsUpdatedRecord() {
	//arrange
	shipment1 := suite.getTestShipment1()
	suite.mock.
		ExpectExec(`UPDATE "shipments"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows(suite.getShipmentColumns()).
		AddRow(suite.getShipmentRow(shipment1)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipments" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shipment, err := suite.repository.UpdateShipment(shipment1)

	//assert
	suite.assert.Nil(err)
	shipment1.CreatedAt = shipment.CreatedAt
	shipment1.UpdatedAt = shipment.UpdatedAt
	suite.assert.Equal(shipment1, shipment)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentRepositoryTestSuite) Test_DeleteShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "shipments" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	err := suite.repository.DeleteShipment(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentRepositoryTestSuite) Test_DeleteShipment_Found_ReturnsNoError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "shipments" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.repository.DeleteShipment(1)

	//assert
	suite.assert.Nil(err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShipmentRepositoryTestSuite) getTestShipment1() *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: uint(1)}, uint(1), "BR1002", models.ShipmentStatePending,
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *ShipmentRepositoryTestSuite) getTestShipment2() *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: uint(2)}, uint(1), "BR1002", models.ShipmentStatePending,
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *ShipmentRepositoryTestSuite) getShipmentColumns() []string {
	return []string{"id", "shipping_method_id", "address_id", "reference_number", "state", "shipment_date",
		"estimated_arrival", "delivered_date", "tracking_number", "created_at", "updated_at", "deleted_at"}
}

func (suite *ShipmentRepositoryTestSuite) getShipmentRow(shipment *models.Shipment) []driver.Value {
	return []driver.Value{shipment.ID, shipment.ShippingMethodID, shipment.AddressID, shipment.ReferenceNumber,
		[]uint8(shipment.State), nil, nil, nil, nil, shipment.CreatedAt, shipment.UpdatedAt, shipment.DeletedAt}
}
