package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

const (
	ErrorShipmentLineItemNotFound = "Shipment line item with id=%d not found"
	shipmentLineItemEntity = "shipmentLineItem"
)

type IShipmentLineItemRepository interface {
	GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, exceptions.IException)
	CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException)
	UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException)
	DeleteShipmentLineItem(id uint) exceptions.IException
}

type shipmentLineItemRepository struct {
	db *gorm.DB
}

func NewShipmentLineItemRepository(db *gorm.DB) IShipmentLineItemRepository {
	return &shipmentLineItemRepository{db}
}

func (repository *shipmentLineItemRepository) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, exceptions.IException) {
	var shipmentLineItems []*models.ShipmentLineItem

	err := repository.db.Where("shipment_id = ?", id).Find(&shipmentLineItems).Error

	return shipmentLineItems, NewDatabaseException(err)
}

func (repository *shipmentLineItemRepository) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException) {
	err := repository.db.Create(shipmentLineItem).Error

	if err != nil {
		return nil, NewDatabaseException(err)
	}

	return repository.getShipmentLineItemByID(shipmentLineItem.ID)
}

func (repository *shipmentLineItemRepository) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException) {
	result := repository.db.Model(&shipmentLineItem).Updates(shipmentLineItem)

	if result.Error != nil {
		return nil, NewDatabaseException(result.Error)
	}

	if result.RowsAffected == 0 {
		return nil, NewEntityNotFound(shipmentLineItemEntity, string(shipmentLineItem.ID), fmt.Errorf(ErrorShipmentLineItemNotFound, shipmentLineItem.ID))
	}

	return repository.getShipmentLineItemByID(shipmentLineItem.ID)
}

func (repository *shipmentLineItemRepository) DeleteShipmentLineItem(id uint) exceptions.IException {
	res := repository.db.Delete(&models.ShipmentLineItem{}, id)

	if res.Error != nil {
		return NewDatabaseException(res.Error)
	}

	if res.RowsAffected == 0 {
		return NewEntityNotFound(shipmentLineItemEntity, string(id), fmt.Errorf(ErrorShipmentLineItemNotFound, id))
	}

	return nil
}

func (repository *shipmentLineItemRepository) getShipmentLineItemByID(id uint) (*models.ShipmentLineItem, exceptions.IException) {
	var shipmentLineItem models.ShipmentLineItem

	if err := repository.db.First(&shipmentLineItem, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, NewEntityNotFound(shipmentLineItemEntity, string(id), fmt.Errorf(ErrorShipmentLineItemNotFound, id))
		}

		return nil, NewDatabaseException(err)
	}

	return &shipmentLineItem, nil
}
