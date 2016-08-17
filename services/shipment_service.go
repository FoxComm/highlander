package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type shipmentService struct {
	db                      *gorm.DB
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
	db *gorm.DB,
	repository repositories.IShipmentRepository,
	addressService IAddressService,
	shipmentLineItemService IShipmentLineItemService,
) IShipmentService {
	return &shipmentService{db, repository, addressService, shipmentLineItemService}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	return service.repository.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
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

	lineItemRepo := repositories.NewShipmentLineItemRepository(txn)

	createdLineItems := []models.ShipmentLineItem{}
	for i, _ := range shipment.ShipmentLineItems {
		shipment.ShipmentLineItems[i].ShipmentID = result.ID

		createdLineItem, err := lineItemRepo.CreateShipmentLineItem(&shipment.ShipmentLineItems[i])
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		createdLineItems = append(createdLineItems, *createdLineItem)
	}

	shipment.Address = *address
	shipment.ShipmentLineItems = createdLineItems

	err = txn.Commit().Error
	return shipment, err
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	_, err := service.repository.UpdateShipment(shipment)
	if err != nil {
		return nil, err
	}

	for i, _ := range shipment.ShipmentLineItems {
		service.shipmentLineItemService.UpdateShipmentLineItem(&shipment.ShipmentLineItems[i])
	}

	return service.repository.GetShipmentByID(shipment.ID)
}
