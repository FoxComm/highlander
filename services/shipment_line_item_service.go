package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

 type shipmentLineItemService struct {
 	repository repositories.IShipmentLineItemRepository
 }

type IShipmentLineItemService interface {
	GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error)
	CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
	UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
}

 func NewShipmentLineItemService(repository repositories.IShipmentLineItemRepository) IShipmentLineItemService {
 	return &shipmentLineItemService{repository}
 }

 func (service *shipmentLineItemService) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error) {
 	return service.repository.GetShipmentLineItemsByShipmentID(id)
 }

 func (service *shipmentLineItemService) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
 	return service.repository.CreateShipmentLineItem(shipmentLineItem)
 }

func (service *shipmentLineItemService) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	return service.repository.UpdateShipmentLineItem(shipmentLineItem)
}
