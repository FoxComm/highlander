package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type IShipmentRepository interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
	DeleteShipment(id uint) error
}

type shipmentRepository struct {
	db *gorm.DB
}

func NewShipmentRepository(db *gorm.DB) IShipmentRepository {
	return &shipmentRepository{db}
}

func (repository *shipmentRepository) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	var shipments []*models.Shipment

	err := repository.db.Where("reference_number = ?", referenceNumber).Find(&shipments).Error

	return shipments, err
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

func (repository *shipmentRepository) DeleteShipment(id uint) error {
	res := repository.db.Delete(&models.Shipment{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}

	return nil
}

func (repository *shipmentRepository) getShipmentByID(id uint) (*models.Shipment, error) {
	var shipment models.Shipment

	if err := repository.db.First(&shipment, id).Error; err != nil {
		return nil, err
	}

	return &shipment, nil
}
