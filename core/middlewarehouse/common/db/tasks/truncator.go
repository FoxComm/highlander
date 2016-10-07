package tasks

import (
	"fmt"

	"github.com/jinzhu/gorm"
)

func TruncateTables(db *gorm.DB, tables []string) {
	for _, t := range tables {
		db.Exec(fmt.Sprintf("truncate %s RESTART IDENTITY CASCADE", t))
	}
}
