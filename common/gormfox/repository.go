package gormfox

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/jinzhu/gorm"
)

type Repository interface {
	Create(model Model) error
	Find(model Model, associations ...Association) (Model, error)
	FindByID(model Model, id uint, associations ...Association) (Model, error)
	FindAll(models interface{}) (interface{}, error)
	Update(model Model) error
	Delete(model Model) error
}

//A repository object for reuse
type repository struct {
	db *gorm.DB
}

func (r *repository) Create(model Model) error {
	_, err := model.Validate(r)

	if err != nil {
		return err
	}

	return r.db.Create(model).Error
}

func (r *repository) Find(model Model, associations ...Association) (Model, error) {
	err := r.db.First(model).Error

	if err != nil {
		return nil, err
	}

	err = buildAssociations(r.db, model, associations)

	return Model(model), err
}

func (r *repository) FindByID(model Model, id uint, associations ...Association) (Model, error) {
	err := r.db.First(model, id).Error

	if err != nil {
		fmt.Printf("%s\n", err)
		return nil, err
	}

	err = buildAssociations(r.db, model, associations)

	return Model(model), err
}

//FindAll wont work via an abstraction using an array of Models. See http://golang.org/doc/faq#convert_slice_of_interface
func (r *repository) FindAll(models interface{}) (interface{}, error) {
	err := r.db.Find(models).Error

	if err != nil {
		return nil, err
	}

	return models, err
}

func (r *repository) Update(model Model) error {
	_, err := model.Validate(r)

	if err != nil {
		return err
	}

	return r.db.Save(model).Error
}

func (r *repository) Delete(model Model) error {
	return r.db.Delete(model).Error
}

func NewRepository() (*repository, error) {
	db, err := config.DefaultConnection()

	if err != nil {
		return &repository{}, err
	}

	return &repository{
		db: db,
	}, nil
}

func buildAssociations(db *gorm.DB, model Model, associations []Association) error {
	var err error

	for _, association := range associations {
		if association.Relation() == "many2many" {
			many2Many := association.(ManyToMany)
			err = db.Model(model).Related(many2Many.Collection, many2Many.Name).Error
		} else if association.Relation() == "has_many" {
			relationName := association.(HasMany)
			val := string(relationName)
			err = db.Preload(val).Find(model).Error
		} else {
			relationName := association.(HasOne)
			val := string(relationName)
			err = db.Preload(val).Find(model).Error
		}

		if err != nil {
			return err
		}
	}

	return nil
}
