package services

import (
	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/FoxComm/highlander/remote/utils/failures"
	"github.com/jinzhu/gorm"
)

func FindChannelByID(phxDB *gorm.DB, id int, phxChannel *phoenix.Channel) failures.Failure {
	params := map[string]interface{}{
		"model": "channel",
		"id":    id,
	}

	return failures.New(phxDB.First(phxChannel, id).Error, params)
}

func InsertChannel(phxDB *gorm.DB, phxChannel *phoenix.Channel) failures.Failure {
	return failures.New(phxDB.Create(phxChannel).Error, nil)
}

func UpdateChannel(phxDB *gorm.DB, phxChannel *phoenix.Channel) failures.Failure {
	return failures.New(phxDB.Save(phxChannel).Error, nil)
}
