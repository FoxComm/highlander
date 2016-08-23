package services

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
)

type shipmentService struct {
	db *gorm.DB
}

type IShipmentService interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(db *gorm.DB) IShipmentService {
	return &shipmentService{db}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	repo := repositories.NewShipmentRepository(service.db)
	return repo.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	unitRepo := repositories.NewStockItemUnitRepository(txn)
	stockItemUnits, err := unitRepo.GetUnitsInOrder(shipment.ReferenceNumber)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	addressRepo := repositories.NewAddressRepository(txn)
	address, err := addressRepo.CreateAddress(&shipment.Address)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	shipmentRepo := repositories.NewShipmentRepository(txn)

	shipment.AddressID = address.ID
	result, err := shipmentRepo.CreateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	shipment.ID = result.ID
	lineItemRepo := repositories.NewShipmentLineItemRepository(txn)

	createdLineItems := []models.ShipmentLineItem{}
	boundItems := make(map[uint]uint)
	for i, _ := range shipment.ShipmentLineItems {
		lineItem := &shipment.ShipmentLineItems[i]
		lineItem.ShipmentID = shipment.ID

		stockItemUnit, err := service.getStockItemUnitForShipmentLineItem(shipment.ReferenceNumber, lineItem.SKU, boundItems, stockItemUnits)
		if err != nil {
			txn.Rollback()
			return nil, err
		}
		lineItem.StockItemUnitID = stockItemUnit.ID
		createdLineItem, err := lineItemRepo.CreateShipmentLineItem(lineItem)
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		createdLineItems = append(createdLineItems, *createdLineItem)
	}

	if _, err = unitRepo.ReserveUnitsInOrder(shipment.ReferenceNumber); err != nil {
		txn.Rollback()
		return nil, err
	}

	shipment.Address = *address
	shipment.ShipmentLineItems = createdLineItems

	err = txn.Commit().Error
	return shipment, err
}

func (service *shipmentService) getStockItemUnitForShipmentLineItem(
	referenceNumber string,
	sku string,
	boundItems map[uint]uint,
	stockItemUnits []*models.StockItemUnit,
) (*models.StockItemUnit, error) {
	for _, stockItemUnit := range stockItemUnits {
		if _, bound := boundItems[stockItemUnit.ID]; stockItemUnit.StockItem.SKU == sku && !bound {
			boundItems[stockItemUnit.ID] = stockItemUnit.ID
			return stockItemUnit, nil
		}
	}

	return nil, fmt.Errorf("Not found stock item unit with reference number %s and sku %s", referenceNumber, sku)
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

	if shipment.State != source.State && shipment.State == models.ShipmentStateCancelled {
		unitRepo := repositories.NewStockItemUnitRepository(txn)
		if _, err = unitRepo.UnsetUnitsInOrder(shipment.ReferenceNumber); err != nil {
			txn.Rollback()
			return nil, err
		}
	}

	err = txn.Commit().Error
	return shipment, err
}
