package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

const (
	ErrorShipmentNotFound = "Shipment with id=%d not found"
	shipmentEntity = "shipment"
)

type IShipmentRepository interface {
	GetShipmentsByOrder(orderRefNum string) ([]*models.Shipment, exceptions.IException)
	GetShipmentByID(ref uint) (*models.Shipment, exceptions.IException)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException)
	DeleteShipment(id uint) exceptions.IException
}

type shipmentRepository struct {
	db *gorm.DB
}

func NewShipmentRepository(db *gorm.DB) IShipmentRepository {
	return &shipmentRepository{db}
}

func (repository *shipmentRepository) GetShipmentsByOrder(orderRefNum string) ([]*models.Shipment, exceptions.IException) {
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

	return shipments, NewDatabaseException(err)
}

func (repository *shipmentRepository) GetShipmentByID(id uint) (*models.Shipment, exceptions.IException) {
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
			return nil, NewEntityNotFound(shipmentEntity, string(id), fmt.Errorf(ErrorShipmentNotFound, id))
		}

		return nil, NewDatabaseException(err)
	}

	return &shipment, nil
}

func (repository *shipmentRepository) CreateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	err := repository.db.Create(shipment).Error

	if err != nil {
		return nil, NewDatabaseException(err)
	}

	return repository.GetShipmentByID(shipment.ID)
}

func (repository *shipmentRepository) UpdateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
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
		return nil, NewDatabaseException(result.Error)
	}

	if result.RowsAffected == 0 {
		return nil, NewEntityNotFound(shipmentEntity, string(shipment.ID), fmt.Errorf(ErrorShipmentNotFound, shipment.ID))
	}

	return repository.GetShipmentByID(shipment.ID)
}

func (repository *shipmentRepository) DeleteShipment(id uint) exceptions.IException {
	res := repository.db.Delete(&models.Shipment{}, id)

	if res.Error != nil {
		return NewDatabaseException(res.Error)
	}

	if res.RowsAffected == 0 {
		return NewEntityNotFound(shipmentEntity, string(id), fmt.Errorf(ErrorShipmentNotFound, id))
	}

	return nil
}
