package main

import (
	"bufio"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type productVariant struct {
	ID         int
	Code       string
	ContextID  int
	ShadowID   int
	FormID     int
	CommitID   int
	CreatedAt  time.Time
	UpdatedAt  time.Time
	ArchivedAt *time.Time
	Scope      string
}

type productVariantMwhSkuID struct {
	ID            int
	VariantFormID int
	MwhSkuID      int
	CreatedAt     time.Time
}

func main() {
	reader := bufio.NewReader(os.Stdin)
	fmt.Print("DB Host: ")
	dbHost, err := reader.ReadString('\n')
	if err != nil {
		log.Fatal(err)
	}

	fmt.Print("Phoenix Database: ")
	phoenixDB, err := reader.ReadString('\n')
	if err != nil {
		log.Fatal(err)
	}

	fmt.Print("Phoenix User: ")
	phoenixUser, err := reader.ReadString('\n')
	if err != nil {
		log.Fatal(err)
	}

	fmt.Print("Middlewarehouse Database: ")
	mwhDB, err := reader.ReadString('\n')
	if err != nil {
		log.Fatal(err)
	}

	fmt.Print("Middlewarehouse User: ")
	mwhUser, err := reader.ReadString('\n')
	if err != nil {
		log.Fatal(err)
	}

	phoenixConfig := config.NewPGConfig()
	phoenixConfig.Host = dbHost
	phoenixConfig.DatabaseName = phoenixDB
	phoenixConfig.User = phoenixUser
	phoenixConnection, err := config.Connect(phoenixConfig)
	if err != nil {
		log.Fatal(err)
	}

	mwhConfig := config.NewPGConfig()
	mwhConfig.Host = dbHost
	mwhConfig.DatabaseName = mwhDB
	mwhConfig.User = mwhUser
	mwhConnection, err := config.Connect(mwhConfig)
	if err != nil {
		log.Fatal(err)
	}

	var variants []*productVariant
	if err := phoenixConnection.Find(&variants).Error; err != nil {
		log.Fatal(err)
	}

	for _, variant := range variants {
		fmt.Printf("Migrating variant with code: %s, form ID: %d, and scope: %s\n", variant.Code, variant.FormID, variant.Scope)
		sku := models.SKU{
			Scope:            variant.Scope,
			Code:             variant.Code,
			RequiresShipping: true,
			ShippingClass:    "default",
		}

		err := mwhConnection.Create(&sku).Error
		if err != nil {
			log.Fatal(err)
		}

		mapping := productVariantMwhSkuID{
			VariantFormID: variant.FormID,
			MwhSkuID:      int(sku.ID),
		}

		if err := phoenixConnection.Create(&mapping).Error; err != nil {
			log.Fatal(err)
		}
	}
}
