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
	return service.shipmentRepository.CreateShipment(shipment)
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	return service.shipmentRepository.UpdateShipment(shipment)
}
