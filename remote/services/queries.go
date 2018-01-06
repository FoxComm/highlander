package services

import (
	"github.com/FoxComm/highlander/remote/models/ic"
	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/payloads"
	"github.com/FoxComm/highlander/remote/responses"
	"github.com/FoxComm/highlander/remote/utils/failures"
)

func FindChannelByID(dbs *RemoteDBs, id int) (*responses.Channel, failures.Failure) {
	var phxChannel *phoenix.Channel
	if fail := dbs.Phx().FindByID(id, phxChannel); fail != nil {
		return nil, fail
	}

	var icChannel *ic.Channel
	fail := dbs.IC().FindByIDWithFailure(
		phxChannel.IntelligenceChannelID,
		icChannel,
		failures.FailureBadRequest)

	if fail != nil {
		return nil, fail
	}

	var hostMaps []*ic.HostMap
	if fail := dbs.IC().FindWhere("channel_id", icChannel.ID, &hostMaps); fail != nil {
		return nil, fail
	}

	return responses.NewChannel(icChannel, phxChannel, hostMaps), nil
}

func InsertChannel(dbs *RemoteDBs, payload *payloads.CreateChannel) (*responses.Channel, failures.Failure) {
	icChannel := payload.IntelligenceModel()
	phxChannel := payload.PhoenixModel()
	hostMaps := icChannel.HostMaps(payload.Hosts, phxChannel.Scope)

	var phxOrganization phoenix.Organization

	txn := dbs.Begin()

	if fail := txn.Phx().FindByIDWithFailure(icChannel.OrganizationID, &phxOrganization, failures.FailureBadRequest); fail != nil {
		txn.Rollback()
		return nil, fail
	}

	if fail := txn.Phx().Create(phxChannel); fail != nil {
		txn.Rollback()
		return nil, fail
	}

	if fail := txn.IC().Create(icChannel); fail != nil {
		txn.Rollback()
		return nil, fail
	}

	// We have to iterate through each insert manually because of a limitation in
	// Gorm. Since there will rarely be many hosts created at a time, this should
	// be a workable solution for now.
	for idx := range hostMaps {
		if fail := txn.IC().CreateWithTable(hostMaps[idx], "host_map"); fail != nil {
			txn.Rollback()
			return nil, fail
		}
	}

	if fail := txn.Commit(); fail != nil {
		return nil, fail
	}

	return responses.NewChannel(icChannel, phxChannel, hostMaps), nil
}

func compareHosts(currentHMs []*ic.HostMap, newHMs []string) ([]*ic.HostMap, []*ic.HostMap) {
	existing := map[string]*ic.HostMap{}
	for _, host := range currentHMs {
		existing[host.Host] = host
	}

	toAdd := []*ic.HostMap{}
	for _, newHost := range newHMs {
		if _, ok := existing[newHost]; !ok {
			hm := &ic.HostMap{Host: newHost}
			toAdd = append(toAdd, hm)
			delete(existing, newHost)
		}
	}

	toRemove := []*ic.HostMap{}
	for _, oldHost := range existing {
		toRemove = append(toRemove, oldHost)
	}

	return toAdd, toRemove
}

func UpdateChannel(dbs *RemoteDBs, id int, payload *payloads.UpdateChannel) (*responses.Channel, failures.Failure) {
	var origPhx *phoenix.Channel
	if fail := dbs.Phx().FindByID(id, origPhx); fail != nil {
		return nil, fail
	}

	txn := dbs.Begin()

	newPhx := payload.PhoenixModel(origPhx)
	if fail := txn.Phx().Save(newPhx); fail != nil {
		txn.Rollback()
		return fail
	}

	if payload.Hosts != nil {
		var origHost []*ic.HostMap

	}

	if fail := dbs.Commit(); fail != nil {
		return nil, fail
	}

	// 	if payload.Hosts != nil {
	// 		newHost := *(payload.Hosts)
	// 		toAdd, toDelete := compareHosts(origHost, newHost)

	// 		for _, host := range toAdd {
	// 			host.ChannelID = origIC.ID
	// 			host.Scope = newPhx.Scope

	// 			if fail := txn.IC().CreateWithTable(host, "host_map"); fail != nil {
	// 				txn.Rollback()
	// 				return fail
	// 			}
	// 		}

	// 		for _, host := range toDelete {
	// 			if fail := txn.IC().Delete(host); fail != nil {
	// 				txn.Rollback()
	// 				return fail
	// 			}
	// 		}
	// 	}

	// 	return dbs.Commit()
	return nil, nil
}
