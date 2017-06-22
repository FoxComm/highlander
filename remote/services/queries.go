package services

import (
	"github.com/FoxComm/highlander/remote/models/ic"
	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/utils/failures"
)

func FindChannelByID(dbs *RemoteDBs, id int, phxChannel *phoenix.Channel) failures.Failure {
	return dbs.Phx().FindByID("channel", id, phxChannel)
}

func InsertChannel(dbs *RemoteDBs, icChannel *ic.Channel, phxChannel *phoenix.Channel, hosts []string) failures.Failure {
	var phxOrganization phoenix.Organization

	txn := dbs.Begin()

	if fail := txn.Phx().FindByIDWithFailure("organization", icChannel.OrganizationID, &phxOrganization, failures.FailureBadRequest); fail != nil {
		txn.Rollback()
		return fail
	}

	if fail := txn.Phx().Create(phxChannel); fail != nil {
		txn.Rollback()
		return fail
	}

	if fail := txn.IC().Create(icChannel); fail != nil {
		txn.Rollback()
		return fail
	}

	hostMaps := icChannel.HostMaps(hosts, phxChannel.Scope)
	// We have to iterate through each insert manually because of a limitation in
	// Gorm. Since there will rarely be many hosts created at a time, this should
	// be a workable solution for now.
	for idx := range hostMaps {
		if fail := txn.IC().CreateWithTable(hostMaps[idx], "host_map"); fail != nil {
			txn.Rollback()
			return fail
		}
	}

	return txn.Commit()
}

func UpdateChannel(dbs *RemoteDBs, phxChannel *phoenix.Channel) failures.Failure {
	return dbs.Phx().Save(phxChannel)
}
