package repositories

import (
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/store"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	_ "github.com/lib/pq"
)

// StockItemRepository manages persistence for StockItems
type StockItemRepository interface {
	Create(*models.StockItem) error
	Update(*models.StockItem) error
	Find(uint) (*models.StockItem, error)
}

func NewStockItemRepository(tc *store.StoreContext) (StockItemRepository, error) {
	db, err := config.DefaultConnection()
	return &pgStockItemRepository{DB: db}, err
}

type pgStockItemRepository struct {
	DB *gorm.DB
}

func (p *pgStockItemRepository) Create(item *models.StockItem) error {
	return p.DB.Create(item).Error
}

func (p *pgStockItemRepository) Update(item *models.StockItem) error {
	return p.DB.Save(item).Error
}

func (p *pgStockItemRepository) Find(id uint) (*models.StockItem, error) {
	item := &models.StockItem{}
	err := p.DB.First(item, id).Error
	return item, err
}
