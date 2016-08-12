package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
)

type shipmentService struct {
	repository              repositories.IShipmentRepository
	addressService          IAddressService
	shipmentLineItemService IShipmentLineItemService
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
) IShipmentService {
	return &shipmentService{repository, addressService, shipmentLineItemService}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	return service.repository.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	address, err := service.addressService.CreateAddress(&shipment.Address)
	if err != nil {
		return nil, err
	}

	shipment.AddressID = address.ID
	shipment, err = service.repository.CreateShipment(shipment)
	if err != nil {
		service.addressService.DeleteAddress(address.ID)
		return nil, err
	}

	createdLineItems := []models.ShipmentLineItem{}
	for _, lineItem := range shipment.ShipmentLineItems {
		lineItem.ShipmentID = shipment.ID
		createdLineItem, err := service.shipmentLineItemService.CreateShipmentLineItem(&lineItem)
		if err != nil {
			service.repository.DeleteShipment(shipment.ID)
			service.addressService.DeleteAddress(address.ID)
			for _, lineItem = range createdLineItems {
				service.shipmentLineItemService.DeleteShipmentLineItem(lineItem.ID)
			}

			return nil, err
		}

		createdLineItems = append(createdLineItems, *createdLineItem)
	}

	return service.repository.GetShipmentByID(shipment.ID)
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	shipment, err := service.repository.UpdateShipment(shipment)
	if err != nil {
		return nil, err
	}

	for _, lineItem := range shipment.ShipmentLineItems {
		service.shipmentLineItemService.UpdateShipmentLineItem(&lineItem)
	}

	return service.repository.GetShipmentByID(shipment.ID)
}
