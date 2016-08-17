package services

import (
	"fmt"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type shipmentService struct {
	shipmentRepository      repositories.IShipmentRepository
	addressService          IAddressService
	shipmentLineItemService IShipmentLineItemService
	stockItemUnitRepository repositories.IStockItemUnitRepository
}

type IShipmentService interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(
	repository repositories.IShipmentRepository,
	addressService IAddressService,
	shipmentLineItemService IShipmentLineItemService,
	stockItemUnitRepository repositories.IStockItemUnitRepository,
) IShipmentService {
	return &shipmentService{
		repository,
		addressService,
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

	address, err := service.addressService.CreateAddress(&shipment.Address)
	if err != nil {
		return nil, err
	}

	shipment.AddressID = address.ID
	result, err := service.shipmentRepository.CreateShipment(shipment)
	if err != nil {
		service.addressService.DeleteAddress(address.ID)
		return nil, err
	}

	createdLineItems := []models.ShipmentLineItem{}
	boundItems := make(map[uint]uint)
	for i, _ := range shipment.ShipmentLineItems {
		var createdLineItem *models.ShipmentLineItem
		lineItem := &shipment.ShipmentLineItems[i]
		lineItem.ShipmentID = result.ID

		stockItemUnit := service.getStockItemUnitForShipmentLineItem(lineItem.SKU, boundItems, stockItemUnits)
		if stockItemUnit == nil {
			err = fmt.Errorf("Not found stock item unit with reference number %s and sku %s", shipment.ReferenceNumber, lineItem.SKU)
		} else {
			lineItem.StockItemUnitID = stockItemUnit.ID
			createdLineItem, err = service.shipmentLineItemService.CreateShipmentLineItem(lineItem)
		}

		if err != nil {
			service.shipmentRepository.DeleteShipment(result.ID)
			service.addressService.DeleteAddress(address.ID)
			for _, lineItem := range createdLineItems {
				service.shipmentLineItemService.DeleteShipmentLineItem(lineItem.ID)
			}

			return nil, err
		}

		createdLineItems = append(createdLineItems, *createdLineItem)
	}

	return service.shipmentRepository.GetShipmentByID(result.ID)
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
	_, err := service.shipmentRepository.UpdateShipment(shipment)
	if err != nil {
		return nil, err
	}

	return service.shipmentRepository.GetShipmentByID(shipment.ID)
}
