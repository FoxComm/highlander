package services

import (
	"fmt"

	"github.com/FoxComm/highlander/remote/models/ic"
	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/utils/failures"
	"github.com/jinzhu/gorm"
)

func FindChannelByID(phxDB *gorm.DB, id int, phxChannel *phoenix.Channel) failures.Failure {
	return findByID(phxDB, "channel", id, phxChannel)
}

func InsertChannel(icDB *gorm.DB, phxDB *gorm.DB, icChannel *ic.Channel, phxChannel *phoenix.Channel, hosts []string) failures.Failure {
	var phxOrganization phoenix.Organization

	phxTxn := phxDB.Begin()
	icTxn := icDB.Begin()

	if fail := internalFindByID(phxTxn, "organization", icChannel.OrganizationID, &phxOrganization); fail != nil {
		phxTxn.Rollback()
		icTxn.Rollback()
		return fail
	}

	if fail := failures.New(phxTxn.Create(phxChannel).Error); fail != nil {
		phxTxn.Rollback()
		icTxn.Rollback()
		return fail
	}

	if fail := failures.New(icTxn.Create(icChannel).Error); fail != nil {
		phxTxn.Rollback()
		icTxn.Rollback()
		return fail
	}

	hostMaps := icChannel.HostMaps(hosts, "1")
	// We have to iterate through each insert manually because of a limitation in
	// Gorm. Since there will rarely be many hosts created at a time, this should
	// be a workable solution for now.
	for idx := range hostMaps {
		if fail := failures.New(icTxn.Table("host_map").Create(hostMaps[idx]).Error); fail != nil {
			phxTxn.Rollback()
			icTxn.Rollback()
			return fail
		}
	}

	if fail := failures.New(icTxn.Commit().Error); fail != nil {
		phxTxn.Rollback()
		return fail
	}

	return failures.New(phxTxn.Commit().Error)
}

func UpdateChannel(phxDB *gorm.DB, phxChannel *phoenix.Channel) failures.Failure {
	return failures.New(phxDB.Save(phxChannel).Error)
}

func findByID(phxDB *gorm.DB, tableName string, id int, model interface{}) failures.Failure {
	res := phxDB.First(model, id)

	if res.RecordNotFound() {
		return failures.NewModelNotFoundFailure(tableName, id)
	}

	return failures.New(res.Error)
}

func internalFindByID(db *gorm.DB, table string, id, model interface{}) failures.Failure {
	res := db.First(model, id)

	if res.RecordNotFound() {
		err := fmt.Errorf("%s with id %d was not found", table, id)
		return failures.NewGeneralFailure(err, failures.FailureBadRequest)
	}

	return failures.New(res.Error)
}
