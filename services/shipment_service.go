package services

import (
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
	for i, _ := range shipment.ShipmentLineItems {
		lineItem := &shipment.ShipmentLineItems[i]
		lineItem.ShipmentID = result.ID
		createdLineItem, err := service.shipmentLineItemService.CreateShipmentLineItem(lineItem)
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

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	_, err := service.shipmentRepository.UpdateShipment(shipment)
	if err != nil {
		return nil, err
	}

	for i, _ := range shipment.ShipmentLineItems {
		service.shipmentLineItemService.UpdateShipmentLineItem(&shipment.ShipmentLineItems[i])
	}

	return service.shipmentRepository.GetShipmentByID(shipment.ID)
}
