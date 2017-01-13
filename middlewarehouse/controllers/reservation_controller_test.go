package controllers

import (
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type reservationControllerTestSuite struct {
	GeneralControllerTestSuite
	db             *gorm.DB
	service        services.InventoryService
	summaryService services.SummaryService
	stockLocation  *models.StockLocation
	stockItem      *models.StockItem
}

func TestReservationControllerSuite(t *testing.T) {
	suite.Run(t, new(reservationControllerTestSuite))
}

func (suite *reservationControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.Default()

	suite.service = services.NewInventoryService(suite.db)

	controller := NewReservationController(suite.service)
	controller.SetUp(suite.router.Group("/reservations"))

	tasks.TruncateTables(suite.db, []string{
		"stock_locations",
	})

	suite.stockLocation = fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(suite.stockLocation).Error)
}

func (suite *reservationControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"inventory_search_view",
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
	})

	stockItem := models.StockItem{
		SKU:             "TEST-SKU",
		StockLocationID: suite.stockLocation.ID,
		DefaultUnitCost: 0,
	}

	var err error
	suite.stockItem, err = suite.service.CreateStockItem(&stockItem)
	suite.Nil(err)

	units := []*models.StockItemUnit{}
	for i := 0; i < 10; i++ {
		unit := models.StockItemUnit{
			StockItemID: suite.stockItem.ID,
			UnitCost:    0,
			Status:      models.StatusOnHand,
			Type:        models.Sellable,
		}
		units = append(units, &unit)
	}

	suite.Nil(suite.service.IncrementStockItemUnits(suite.stockItem.ID, models.Sellable, units))
}

func (suite *reservationControllerTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *reservationControllerTestSuite) Test_ReserveItems() {
	payload := payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{
				Qty: 2,
				SKU: "TEST-SKU",
			},
		},
	}

	res := suite.Post("/reservations/hold", payload)
	suite.Equal(http.StatusNoContent, res.Code)

	units := []*models.StockItemUnit{}
	err := suite.db.
		Where("stock_item_id = ?", suite.stockItem.ID).
		Where("type = ?", models.Sellable).
		Where("status = ?", models.StatusOnHold).
		Where("ref_num = ?", payload.RefNum).
		Find(&units).
		Error
	suite.Nil(err)
	suite.Equal(2, len(units))
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_WrongSKUs() {
	payload := payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{
				Qty: 2,
				SKU: "TEST-SKEW",
			},
		},
	}

	res := suite.Post("/reservations/hold", payload)
	suite.Equal(http.StatusBadRequest, res.Code)
	suite.Contains(res.Body.String(), "errors")
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_EmptySKUsList() {
	payload := payloads.Reservation{
		RefNum: "BR10001",
		Items:  []payloads.ItemReservation{},
	}

	res := suite.Post("/reservations/hold", payload)

	suite.Equal(http.StatusBadRequest, res.Code)
	suite.Contains(res.Body.String(), "errors")
	suite.Contains(res.Body.String(), "Reservation must have at least one SKU")
}

func (suite *reservationControllerTestSuite) Test_ReleaseItems() {
	payload := payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{
				Qty: 2,
				SKU: "TEST-SKU",
			},
		},
	}

	res := suite.Post("/reservations/hold", payload)
	suite.Equal(http.StatusNoContent, res.Code)

	res = suite.Delete("/reservations/hold/BR10001")
	suite.Equal(http.StatusNoContent, res.Code)

	units := []*models.StockItemUnit{}
	err := suite.db.
		Where("stock_item_id = ?", suite.stockItem.ID).
		Where("type = ?", models.Sellable).
		Where("status = ?", models.StatusOnHold).
		Where("ref_num = ?", payload.RefNum).
		Find(&units).
		Error

	suite.Nil(err)
	suite.Equal(0, len(units))
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_WrongRefNum() {
	res := suite.Delete("/reservations/hold/BR10001")

	suite.Equal(http.StatusBadRequest, res.Code)
	suite.Contains(res.Body.String(), "errors")
}
