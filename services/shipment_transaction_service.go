package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	// "github.com/FoxComm/middlewarehouse/repositories"
)

// type shipmentTransactionService struct {
// 	repository repositories.IShipmentTransactionRepository
// }

type IShipmentTransactionService interface {
	GetShipmentTransactionsByShipmentID(id uint) ([]*models.ShipmentTransaction, error)
	GetShipmentTransactionByID(id uint) (*models.ShipmentTransaction, error)
	CreateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, error)
	UpdateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, error)
	DeleteShipmentTransaction(id uint) error
}

// func NewShipmentTransactionService(repository repositories.IShipmentTransactionRepository) IShipmentTransactionService {
// 	return &shipmentTransactionService{repository}
// }

// func (service *shipmentTransactionService) GetShipmentTransactions() ([]*models.ShipmentTransaction, error) {
// 	return service.repository.GetShipmentTransactions()
// }

// func (service *shipmentTransactionService) GetShipmentTransactionByID(id uint) (*models.ShipmentTransaction, error) {
// 	return service.repository.GetShipmentTransactionByID(id)
// }

// func (service *shipmentTransactionService) CreateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, error) {
// 	return service.repository.CreateShipmentTransaction(shipmentTransaction)
// }

// func (service *shipmentTransactionService) UpdateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, error) {
// 	return service.repository.UpdateShipmentTransaction(shipmentTransaction)
// }

// func (service *shipmentTransactionService) DeleteShipmentTransaction(id uint) error {
// 	return service.repository.DeleteShipmentTransaction(id)
// }
