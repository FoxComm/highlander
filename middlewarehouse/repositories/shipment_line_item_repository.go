package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorShipmentLineItemNotFound = "Shipment line item with id=%d not found"
)

type IShipmentLineItemRepository interface {
	GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error)
	CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
	UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error)
	DeleteShipmentLineItem(id uint) error
}

type shipmentLineItemRepository struct {
	db *gorm.DB
}

func NewShipmentLineItemRepository(db *gorm.DB) IShipmentLineItemRepository {
	return &shipmentLineItemRepository{db}
}

func (repository *shipmentLineItemRepository) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error) {
	var shipmentLineItems []*models.ShipmentLineItem

	err := repository.db.Where("shipment_id = ?", id).Find(&shipmentLineItems).Error

	return shipmentLineItems, err
}

func (repository *shipmentLineItemRepository) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	fmt.Printf("The shipment line item is:\n%v\n", shipmentLineItem)
	fmt.Printf("StockItemUnitID: %d\n", shipmentLineItem.StockItemUnitID)
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
		return nil, fmt.Errorf(ErrorShipmentLineItemNotFound, shipmentLineItem.ID)
	}

	return repository.getShipmentLineItemByID(shipmentLineItem.ID)
}

func (repository *shipmentLineItemRepository) DeleteShipmentLineItem(id uint) error {
	res := repository.db.Delete(&models.ShipmentLineItem{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return fmt.Errorf(ErrorShipmentLineItemNotFound, id)
	}

	return nil
}

func (repository *shipmentLineItemRepository) getShipmentLineItemByID(id uint) (*models.ShipmentLineItem, error) {
	var shipmentLineItem models.ShipmentLineItem

	if err := repository.db.First(&shipmentLineItem, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorShipmentLineItemNotFound, id)
		}

		return nil, err
	}

	return &shipmentLineItem, nil
}
