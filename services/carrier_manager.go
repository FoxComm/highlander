package services

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/models"
)

func GetCarriers() ([]models.Carrier, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	var carriers []models.Carrier
	db.Find(&carriers)

	return carriers, nil
}

func CreateCarrier(payload *payloads.Carrier) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	carrier := models.NewCarrierFromPayload(payload)

	return db.Create(carrier).Error
}

func UpdateCarrier(payload *payloads.Carrier) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	carrier := models.NewCarrierFromPayload(payload)

	return db.Model(&carrier).Updates(carrier).Error
}

func DeleteCarrier(id uint) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	carrier := new(models.Carrier)
	carrier.ID = id
	return db.Delete(&carrier).Error
}
