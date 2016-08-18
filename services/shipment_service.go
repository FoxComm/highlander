package services

import (
	"fmt"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type shipmentService struct {
	db                      *gorm.DB
	shipmentRepository      repositories.IShipmentRepository
	shipmentLineItemService IShipmentLineItemService
	stockItemUnitRepository repositories.IStockItemUnitRepository
}

type IShipmentService interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(
	db *gorm.DB,
shipmentRepository repositories.IShipmentRepository,
	shipmentLineItemService IShipmentLineItemService,
	stockItemUnitRepository repositories.IStockItemUnitRepository,
) IShipmentService {
	return &shipmentService{
		db,
		shipmentRepository,
		shipmentLineItemService,
		stockItemUnitRepository,
	}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	return service.shipmentRepository.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	stockItemUnits, err := service.stockItemUnitRepository.GetUnitsInOrder(shipment.ReferenceNumber)
	if err != nil {
		return nil, err
	}

	txn := service.db.Begin()

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

		stockItemUnit := service.getStockItemUnitForShipmentLineItem(lineItem.SKU, boundItems, stockItemUnits)
		if stockItemUnit == nil {
			return nil, fmt.Errorf("Not found stock item unit with reference number %s and sku %s", shipment.ReferenceNumber, lineItem.SKU)
		}
		lineItem.StockItemUnitID = stockItemUnit.ID
		createdLineItem, err := lineItemRepo.CreateShipmentLineItem(lineItem)
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		createdLineItems = append(createdLineItems, *createdLineItem)
	}

	if _, err = service.stockItemUnitRepository.ReserveUnitsInOrder(shipment.ReferenceNumber); err != nil {
		txn.Rollback()
		return nil, err
	}

	shipment.Address = *address
	shipment.ShipmentLineItems = createdLineItems

	err = txn.Commit().Error
	return shipment, err
}

func (service *shipmentService) getStockItemUnitForShipmentLineItem(
	sku string,
	boundItems map[uint]uint,
	stockItemUnits []*models.StockItemUnit,
) *models.StockItemUnit {
	for _, stockItemUnit := range stockItemUnits {
		if _, bound := boundItems[stockItemUnit.ID]; stockItemUnit.StockItem.SKU == sku && !bound {
			boundItems[stockItemUnit.ID] = stockItemUnit.ID
			return stockItemUnit
		}
	}

	return nil
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	source, err := service.shipmentRepository.GetShipmentByID(shipment.ID)
	if err != nil {
		return nil, err
	}

	shipment, err = service.shipmentRepository.UpdateShipment(shipment)
	if err != nil {
		return nil, err
	}

	//TODO: transactions
	if shipment.State != source.State && shipment.State == models.ShipmentStateCancelled {
		service.stockItemUnitRepository.UnsetUnitsInOrder(shipment.ReferenceNumber)
	}

	return shipment, nil
}
