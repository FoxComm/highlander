package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	// "github.com/FoxComm/middlewarehouse/repositories"
)

// type shipmentLineItemService struct {
// 	repository repositories.IShipmentLineItemRepository
// }

type IShipmentLineItemService interface {
	GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error)
	GetShipmentLineItemByID(id uint) (*models.ShipmentLineItem, error)
	CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
	UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
	DeleteShipmentLineItem(id uint) error
}

// func NewShipmentLineItemService(repository repositories.IShipmentLineItemRepository) IShipmentLineItemService {
// 	return &shipmentLineItemService{repository}
// }

// func (service *shipmentLineItemService) GetShipmentLineItems() ([]*models.ShipmentLineItem, error) {
// 	return service.repository.GetShipmentLineItems()
// }

// func (service *shipmentLineItemService) GetShipmentLineItemByID(id uint) (*models.ShipmentLineItem, error) {
// 	return service.repository.GetShipmentLineItemByID(id)
// }

// func (service *shipmentLineItemService) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
// 	return service.repository.CreateShipmentLineItem(shipmentLineItem)
// }

// func (service *shipmentLineItemService) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
// 	return service.repository.UpdateShipmentLineItem(shipmentLineItem)
// }

// func (service *shipmentLineItemService) DeleteShipmentLineItem(id uint) error {
// 	return service.repository.DeleteShipmentLineItem(id)
// }
