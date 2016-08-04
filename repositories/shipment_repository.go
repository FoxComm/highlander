package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type IShipmentRepository interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

type shipmentRepository struct {
	db *gorm.DB
}

func NewShipmentRepository(db *gorm.DB) IShipmentRepository {
	return &shipmentRepository{db}
}

func (repository *shipmentRepository) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	var shipments []*models.Shipment

	if err := repository.db.Where("reference_number = ?", referenceNumber).Find(&shipments).Error; err != nil {
		return nil, err
	}

	return shipments, nil
}

func (repository *shipmentRepository) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	err := repository.db.Create(shipment).Error

	if err != nil {
		return nil, err
	}

	return repository.getShipmentByID(shipment.ID)
}

func (repository *shipmentRepository) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	result := repository.db.Model(&shipment).Updates(shipment)

	if result.Error != nil {
		return nil, result.Error
	}

	if result.RowsAffected == 0 {
		return nil, gorm.ErrRecordNotFound
	}

	return repository.getShipmentByID(shipment.ID)
}

func (repository *shipmentRepository) getShipmentByID(id uint) (*models.Shipment, error) {
	var shipment models.Shipment

	if err := repository.db.First(&shipment, id).Error; err != nil {
		return nil, err
	}

	return &shipment, nil
}
