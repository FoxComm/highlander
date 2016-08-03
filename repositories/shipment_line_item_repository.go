package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type IShipmentLineItemRepository interface {
	GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error)
	CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
	UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
}

type shipmentLineItemRepository struct {
	db *gorm.DB
}

func NewShipmentLineItemRepository(db *gorm.DB) IShipmentLineItemRepository {
	return &shipmentLineItemRepository{db}
}

func (repository *shipmentLineItemRepository) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error) {
	var shipmentLineItems []*models.ShipmentLineItem

	if err := repository.db.Where("shipment_id = ?", id).Find(&shipmentLineItems).Error; err != nil {
		return nil, err
	}

	return shipmentLineItems, nil
}

func (repository *shipmentLineItemRepository) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	err := repository.db.Create(shipmentLineItem).Error

	if err != nil {
		return nil, err
	}

	return repository.getShipmentLineItemByID(shipmentLineItem.ID)
}

func (repository *shipmentLineItemRepository) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	result := repository.db.Model(&shipmentLineItem).Updates(shipmentLineItem)

	if result.Error != nil {
		return nil, result.Error
	}

	if result.RowsAffected == 0 {
		return nil, gorm.ErrRecordNotFound
	}

	return repository.getShipmentLineItemByID(shipmentLineItem.ID)
}

func (repository *shipmentLineItemRepository) getShipmentLineItemByID(id uint) (*models.ShipmentLineItem, error) {
	var shipmentLineItem models.ShipmentLineItem

	if err := repository.db.First(&shipmentLineItem, id).Error; err != nil {
		return nil, err
	}

	return &shipmentLineItem, nil
}
