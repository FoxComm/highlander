package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	// "github.com/FoxComm/middlewarehouse/repositories"
)

// type shipmentService struct {
// 	repository repositories.IShipmentRepository
// }

type IShipmentService interface {
	GetShipments() ([]*models.Shipment, error)
	GetShipmentByID(id uint) (*models.Shipment, error)
	CreateShipment(shipment *models.Shipment, address *models.Address, lineItems []*models.ShipmentLineItem) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
	DeleteShipment(id uint) error
}

// func NewShipmentService(repository repositories.IShipmentRepository) IShipmentService {
// 	return &shipmentService{repository}
// }

// func (service *shipmentService) GetShipments() ([]*models.Shipment, error) {
// 	return service.repository.GetShipments()
// }

// func (service *shipmentService) GetShipmentByID(id uint) (*models.Shipment, error) {
// 	return service.repository.GetShipmentByID(id)
// }

// func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
// 	return service.repository.CreateShipment(shipment)
// }

// func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
// 	return service.repository.UpdateShipment(shipment)
// }

// func (service *shipmentService) DeleteShipment(id uint) error {
// 	return service.repository.DeleteShipment(id)
// }
