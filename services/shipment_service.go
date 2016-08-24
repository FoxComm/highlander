package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
)

type shipmentService struct {
	db *gorm.DB
}

type IShipmentService interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(db *gorm.DB) IShipmentService {
	return &shipmentService{db}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	repo := repositories.NewShipmentRepository(service.db)
	return repo.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	unitRepo := repositories.NewStockItemUnitRepository(txn)
	for i, lineItem := range shipment.ShipmentLineItems {
		siu, err := unitRepo.GetUnitForLineItem(shipment.ReferenceNumber, lineItem.SKU)
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		if err := txn.Model(siu).Update("status", "reserved").Error; err != nil {
			txn.Rollback()
			return nil, err
		}

		shipment.ShipmentLineItems[i].StockItemUnitID = siu.ID
	}

	shipmentRepo := repositories.NewShipmentRepository(txn)
	result, err := shipmentRepo.CreateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	err = txn.Commit().Error
	return result, err
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	shipmentRepo := repositories.NewShipmentRepository(txn)
	source, err := shipmentRepo.GetShipmentByID(shipment.ID)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	shipment, err = shipmentRepo.UpdateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if shipment.State != source.State && shipment.State == models.ShipmentStateCancelled {
		unitRepo := repositories.NewStockItemUnitRepository(txn)
		if _, err = unitRepo.UnsetUnitsInOrder(shipment.ReferenceNumber); err != nil {
			txn.Rollback()
			return nil, err
		}
	}

	err = txn.Commit().Error
	return shipment, err
}
