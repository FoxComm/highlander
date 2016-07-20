package services

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/models"
)

func GetCarriers() ([]*models.Carrier, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	var data []models.Carrier
	if err = db.Find(&data).Error; err != nil {
		return nil, err
	}

	carriers := make([]*models.Carrier, len(data))
	for i, _ := range data {
		carriers[i] = &data[i]
	}

	return carriers, nil
}

func GetCarrierById(id uint) (*models.Carrier, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	var carrier models.Carrier
	if err = db.First(&carrier, id).Error; err != nil {
		return nil, err
	}

	return &carrier, nil
}

func CreateCarrier(payload *payloads.Carrier) (uint, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return 0, err
	}

	carrier := models.NewCarrierFromPayload(payload)

	err = db.Create(carrier).Error

	return carrier.ID, err
}

func UpdateCarrier(id uint, payload *payloads.Carrier) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	carrier := models.NewCarrierFromPayload(payload)
	carrier.ID = id

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
