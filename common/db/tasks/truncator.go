package tasks

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/common/db/config"
)

func TruncateTables(tables []string) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	for _, t := range tables {
		db.Exec(fmt.Sprintf("truncate %s RESTART IDENTITY CASCADE", t))
	}
	return nil
}
