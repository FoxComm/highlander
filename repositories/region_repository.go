package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type IRegionRepository interface {
	GetRegionByID(id uint) (*models.Region, error)
}

type regionRepository struct {
	db *gorm.DB
}

func NewRegionRepository(db *gorm.DB) IRegionRepository {
	return &regionRepository{db}
}

func (repository *regionRepository) GetRegionByID(id uint) (*models.Region, error) {
	region := &models.Region{}

	result := repository.db.
		Select([]string{
			"regions.id as id",
			"regions.name as name",
			"countries.id as country_id",
			"countries.name as country_name",
		}).
		Joins("join countries ON regions.country_id=countries.id").
		Where("regions.id=?", id).
		Find(region)

	if result.Error != nil {
		return nil, result.Error
	}

	if result.RowsAffected == 0 {
		return nil, gorm.ErrRecordNotFound
	}

	return region, nil
}
