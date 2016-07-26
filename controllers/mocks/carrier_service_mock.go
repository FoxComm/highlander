package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/jinzhu/gorm"
)

type carrierServiceMock struct {
	id   uint
	rows []*models.Carrier
}

func NewCarrierServiceMock() services.ICarrierService {
	return &carrierServiceMock{}
}

func (service *carrierServiceMock) GetCarriers() ([]*models.Carrier, error) {
	return service.rows, nil
}

func (service *carrierServiceMock) GetCarrierByID(id uint) (*models.Carrier, error) {
	for i := 0; i < len(service.rows); i++ {
		row := service.rows[i]
		if row.ID == id {
			return row, nil
		}
	}

	return nil, gorm.ErrRecordNotFound
}

func (service *carrierServiceMock) CreateCarrier(carrier *models.Carrier) (uint, error) {
	service.id++
	carrier.ID = service.id

	service.rows = append(service.rows, carrier)

	return carrier.ID, nil
}

func (service *carrierServiceMock) UpdateCarrier(carrier *models.Carrier) error {
	for i := 0; i < len(service.rows); i++ {
		row := service.rows[i]
		if row.ID == carrier.ID {
			service.rows[i] = carrier
			return nil
		}
	}

	return gorm.ErrRecordNotFound
}

func (service *carrierServiceMock) DeleteCarrier(id uint) error {
	for i := 0; i < len(service.rows); i++ {
		row := service.rows[i]
		if row.ID == id {
			service.rows = append(service.rows[:i], service.rows[i+1:]...)
			return nil
		}
	}

	return gorm.ErrRecordNotFound
}
