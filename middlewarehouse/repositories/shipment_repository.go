package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorShipmentNotFound = "Shipment with id=%d not found"
)

type IShipmentRepository interface {
	GetShipmentsByOrder(orderRefNum string) ([]*models.Shipment, error)
	GetShipmentByID(ref uint) (*models.Shipment, error)
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

func (repository *shipmentRepository) GetShipmentsByOrder(orderRefNum string) ([]*models.Shipment, error) {
	var shipments []*models.Shipment

	err := repository.db.
		Preload("ShippingMethod").
		Preload("ShippingMethod.Carrier").
		Preload("Address").
		Preload("Address.Region").
		Preload("Address.Region.Country").
		Preload("ShipmentLineItems").
		Where("order_ref_num = ?", orderRefNum).
		Find(&shipments).Error

	return shipments, err
}

func (repository *shipmentRepository) GetShipmentByID(id uint) (*models.Shipment, error) {
	var shipment models.Shipment

	err := repository.db.
		Preload("ShippingMethod").
		Preload("ShippingMethod.Carrier").
		Preload("Address").
		Preload("Address.Region").
		Preload("Address.Region.Country").
		Preload("ShipmentLineItems").
		Preload("ShipmentLineItems.StockItemUnit").
		First(&shipment, id).
		Error

	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorShipmentNotFound, id)
		}

		return nil, err
	}

	return &shipment, nil
}

func (repository *shipmentRepository) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	err := repository.db.Create(shipment).Error

	if err != nil {
		return nil, err
	}

	return repository.GetShipmentByID(shipment.ID)
}

func (repository *shipmentRepository) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	result := repository.db.
		Set("gorm:save_associations", false).
		Model(shipment).
		Select(
			"state",
			"shipment_date",
			"estimated_arrival",
			"delivered_date",
			"tracking_number",
		).
		Save(shipment)

	if result.Error != nil {
		return nil, result.Error
	}

	if result.RowsAffected == 0 {
		return nil, fmt.Errorf(ErrorShipmentNotFound, shipment.ID)
	}

	return repository.GetShipmentByID(shipment.ID)
}

func (repository *shipmentRepository) DeleteShipment(id uint) error {
	res := repository.db.Delete(&models.Shipment{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return fmt.Errorf(ErrorShipmentNotFound, id)
	}

	return nil
}
