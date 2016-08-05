package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	// "github.com/FoxComm/middlewarehouse/repositories"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type shipmentService struct {
	shipmentRepository         repositories.IShipmentRepository
	addressRepository          repositories.IAddressRepository
	shipmentLineItemRepository repositories.IShipmentLineItemRepository
}

type IShipmentService interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment, address *models.Address, lineItems []*models.ShipmentLineItem) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(
	shipmentRepository repositories.IShipmentRepository,
	addressRepository repositories.IAddressRepository,
	shipmentLineItemRepository repositories.IShipmentLineItemRepository,
) IShipmentService {
	return &shipmentService{shipmentRepository, addressRepository, shipmentLineItemRepository}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	return service.shipmentRepository.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(
	shipment *models.Shipment,
	address *models.Address,
	lineItems []*models.ShipmentLineItem,
) (*models.Shipment, error) {
	shiment, err := service.shipmentRepository.CreateShipment(shipment)
	if err != nil {
		return nil, err
	}

	address, err = service.addressRepository.CreateAddress(address)
	if err != nil {
		service.shipmentRepository.DeleteShipment(shiment.ID)
		return nil, err
	}

	createdLineItems := []*models.ShipmentLineItem{}
	for _, lineItem := range lineItems {
		lineItem, err = service.shipmentLineItemRepository.CreateShipmentLineItem(lineItem)
		if err != nil {
			service.shipmentRepository.DeleteShipment(shiment.ID)
			service.addressRepository.DeleteAddress(address.ID)
			for _, lineItem = range createdLineItems {
				service.shipmentLineItemRepository.DeleteShipmentLineItem(lineItem.ID)
			}

			return nil, err
		}
		createdLineItems = append(createdLineItems, lineItem)
	}

	return shiment, err
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	return service.shipmentRepository.UpdateShipment(shipment)
}
