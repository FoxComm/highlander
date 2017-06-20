package services

import (
	"fmt"

	"github.com/FoxComm/highlander/remote/utils"
	"github.com/FoxComm/highlander/remote/utils/failures"
	"github.com/jinzhu/gorm"
)

type RemoteDBs struct {
	icDB  *RemoteDB
	phxDB *RemoteDB
}

func NewRemoteDBs(config *utils.Config) (*RemoteDBs, error) {
	icDB, err := NewIntelligenceConnection(config)
	if err != nil {
		return nil, err
	}

	phxDB, err := NewPhoenixConnection(config)
	if err != nil {
		return nil, err
	}

	return &RemoteDBs{
		icDB:  &RemoteDB{icDB},
		phxDB: &RemoteDB{phxDB},
	}, nil
}

func (r RemoteDBs) Begin() *RemoteDBs {
	return &RemoteDBs{
		icDB:  r.icDB.Begin(),
		phxDB: r.phxDB.Begin(),
	}
}

func (r RemoteDBs) Commit() failures.Failure {
	if fail := r.icDB.Commit(); fail != nil {
		r.phxDB.Rollback()
		return fail
	}

	return r.phxDB.Commit()
}

func (r RemoteDBs) Rollback() {
	r.icDB.Rollback()
	r.phxDB.Rollback()
}

func (r RemoteDBs) IC() *RemoteDB {
	return r.icDB
}

func (r RemoteDBs) Phx() *RemoteDB {
	return r.phxDB
}

type RemoteDB struct {
	db *gorm.DB
}

func (r RemoteDB) Begin() *RemoteDB {
	return &RemoteDB{db: r.db.Begin()}
}

func (r RemoteDB) Commit() failures.Failure {
	return failures.New(r.db.Commit().Error)
}

func (r RemoteDB) Rollback() {
	r.db.Rollback()
}

func (r RemoteDB) Create(model interface{}) failures.Failure {
	return failures.New(r.db.Create(model).Error)
}

func (r RemoteDB) CreateWithTable(model interface{}, table string) failures.Failure {
	return failures.New(r.db.Table(table).Create(model).Error)
}

func (r RemoteDB) FindByID(table string, id int, model interface{}) failures.Failure {
	return r.FindByIDWithFailure(table, id, model, failures.FailureNotFound)
}

func (r RemoteDB) FindByIDWithFailure(table string, id int, model interface{}, failure int) failures.Failure {
	res := r.db.First(model, id)

	if res.RecordNotFound() {
		err := fmt.Errorf("%s with id %d was not found", table, id)
		return failures.NewGeneralFailure(err, failure)
	}

	return failures.New(res.Error)
}

func (r RemoteDB) Save(model interface{}) failures.Failure {
	return failures.New(r.db.Save(model).Error)
}

func (r RemoteDB) Ping() error {
	return r.db.DB().Ping()
}
