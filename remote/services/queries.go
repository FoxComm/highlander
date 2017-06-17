package services

import (
	"fmt"

	"github.com/FoxComm/highlander/remote/models/phoenix"
	"github.com/jinzhu/gorm"
)

func FindChannelByID(phxDB *gorm.DB, id int, phxChannel *phoenix.Channel) error {
	err := phxDB.First(phxChannel, id).Error

	if err != nil && err.Error() == "record not found" {
		return fmt.Errorf("Channel with ID %d not found", id)
	}

	return err
}

func InsertChannel(phxDB *gorm.DB, phxChannel *phoenix.Channel) error {
	return phxDB.Create(phxChannel).Error
}
