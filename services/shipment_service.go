package services

import (
	"github.com/FoxComm/middlewarehouse/common/async"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/models/activities"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
)

type shipmentService struct {
	db                 *gorm.DB
	summaryService     ISummaryService
	activityLogger     IActivityLogger
	updateSummaryAsync bool
}

type IShipmentService interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(db *gorm.DB, summaryService ISummaryService, activityLogger IActivityLogger) IShipmentService {
	return &shipmentService{db, summaryService, activityLogger, true}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	repo := repositories.NewShipmentRepository(service.db)
	return repo.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	stockItemCounts := make(map[uint]int)
	unitRepo := repositories.NewStockItemUnitRepository(txn)
	for i, lineItem := range shipment.ShipmentLineItems {
		siu, err := unitRepo.GetUnitForLineItem(shipment.ReferenceNumber, lineItem.SKU)
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		if err := txn.Model(siu).Update("status", "reserved").Error; err != nil {
			txn.Rollback()
			return nil, err
		}

		shipment.ShipmentLineItems[i].StockItemUnitID = siu.ID

		// Aggregate which stock items, and how many, have been updated, so that we
		// can update summaries asynchronously at the end.
		if count, ok := stockItemCounts[siu.StockItemID]; ok {
			stockItemCounts[siu.StockItemID] = count + 1
		} else {
			stockItemCounts[siu.StockItemID] = 1
		}
	}

	shipmentRepo := repositories.NewShipmentRepository(txn)
	result, err := shipmentRepo.CreateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	err = service.updateSummariesToReserved(stockItemCounts)
	if err != nil {
		return nil, err
	}

	activity, err := activities.NewShipmentCreated(result, result.CreatedAt)
	if err != nil {
		return nil, err
	}

	if err := service.activityLogger.Log(activity); err != nil {
		return nil, err
	}

	return result, nil
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	shipmentRepo := repositories.NewShipmentRepository(txn)
	source, err := shipmentRepo.GetShipmentByID(shipment.ID)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	shipment, err = shipmentRepo.UpdateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	err = service.handleStatusChange(txn, source, shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if err = txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	var activity activities.ISiteActivity
	if source.State != shipment.State && shipment.State == models.ShipmentStateShipped {
		stockItemCounts := make(map[uint]int)
		for _, lineItem := range source.ShipmentLineItems {
			siu := lineItem.StockItemUnit

			// Aggregate which stock items, and how many, have been updated, so that we
			// can update summaries asynchronously at the end.
			if count, ok := stockItemCounts[siu.StockItemID]; ok {
				stockItemCounts[siu.StockItemID] = count + 1
			} else {
				stockItemCounts[siu.StockItemID] = 1
			}
		}

		if err = service.updateSummariesToShipped(stockItemCounts); err != nil {
			return nil, err
		}

		activity, err = activities.NewShipmentUpdated(shipment, shipment.UpdatedAt)
		if err != nil {
			return nil, err
		}
	} else {
		activity, err = activities.NewShipmentUpdated(shipment, shipment.UpdatedAt)
		if err != nil {
			return nil, err
		}
	}

	if err := service.activityLogger.Log(activity); err != nil {
		return nil, err
	}

	return shipment, nil
}

func (service *shipmentService) updateSummariesToReserved(stockItemsMap map[uint]int) error {
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved}
	unitType := models.Sellable

	fn := func() error {
		for id, qty := range stockItemsMap {
			if err := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
				return err
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, "Error updating stock item summary creating shipment")
}

func (service *shipmentService) updateSummariesToShipped(stockItemsMap map[uint]int) error {
	statusShift := models.StatusChange{From: models.StatusReserved, To: models.StatusShipped}
	unitType := models.Sellable

	fn := func() error {
		for id, qty := range stockItemsMap {
			if err := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
				return err
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, "Error updating stock item summary after shipment")
}

func (service *shipmentService) handleStatusChange(db *gorm.DB, oldShipment, newShipment *models.Shipment) error {
	if oldShipment.State == newShipment.State {
		return nil
	}

	unitRepo := repositories.NewStockItemUnitRepository(db)
	var err error

	switch newShipment.State {
	case models.ShipmentStateCancelled:
		_, err = unitRepo.UnsetUnitsInOrder(newShipment.ReferenceNumber)

	case models.ShipmentStateShipped:
		// TODO: Bring capture back when we move to the capture consumer
		unitIDs := []uint{}
		for _, lineItem := range newShipment.ShipmentLineItems {
			unitIDs = append(unitIDs, lineItem.StockItemUnitID)
		}
		err = unitRepo.DeleteUnits(unitIDs)
	}

	return err
}

// func (service *shipmentService) capturePayment(shipment *models.Shipment) error {
// 	// TODO: Move this whole thing to a consumer.
// 	log.Printf("Starting capture")
// 	capture := payloads.Capture{
// 		ReferenceNumber: shipment.ReferenceNumber,
// 		Shipping: payloads.CaptureShippingCost{
// 			Total:    0,
// 			Currency: "USD",
// 		},
// 	}
//
// 	for _, lineItem := range shipment.ShipmentLineItems {
// 		cLineItem := payloads.CaptureLineItem{
// 			ReferenceNumber: lineItem.ReferenceNumber,
// 			SKU:             lineItem.SKU,
// 		}
//
// 		capture.Items = append(capture.Items, cLineItem)
// 	}
//
// 	b, err := json.Marshal(&capture)
// 	if err != nil {
// 		log.Printf("Error marshalling")
// 		return err
// 	}
//
// 	log.Printf("Payload: %s", string(b))
//
// 	url := fmt.Sprintf("%s/v1/service/capture", config.Config.PhoenixURL)
// 	req, err := http.NewRequest("POST", url, bytes.NewBuffer(b))
// 	if err != nil {
// 		log.Printf("Error creating post")
// 		return err
// 	}
//
// 	req.Header.Set("Content-Type", "application/json")
// 	req.Header.Set("JWT", config.Config.PhoenixJWT)
//
// 	client := &http.Client{}
// 	resp, err := client.Do(req)
// 	if err != nil {
// 		log.Printf("Error on the request")
// 		return err
// 	}
//
// 	if resp.StatusCode < 200 || resp.StatusCode > 299 {
// 		msg := fmt.Sprintf("Error in response from capture with status %d", resp.StatusCode)
// 		log.Printf(msg)
// 		return errors.New(msg)
// 	}
//
// 	return nil
// }
