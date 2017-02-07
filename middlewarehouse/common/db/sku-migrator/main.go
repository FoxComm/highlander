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

type productVariantSku struct {
	ID            int
	VariantFormID int
	SkuID         int
	SkuCode       string
	CreatedAt     time.Time
}

func main() {
	// 1. Gather all the parameters needed to connect to the Phoenix and MWH DBs.
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

	// 2. Connect to each DB.
	phoenixConfig := config.NewPGConfig()
	phoenixConfig.Host = dbHost
	phoenixConfig.DatabaseName = phoenixDB
	phoenixConfig.User = phoenixUser
	phoenixConnection, err := config.Connect(phoenixConfig)
	if err != nil {
		log.Fatal(err)
	}
	defer phoenixConnection.Close()

	mwhConfig := config.NewPGConfig()
	mwhConfig.Host = dbHost
	mwhConfig.DatabaseName = mwhDB
	mwhConfig.User = mwhUser
	mwhConnection, err := config.Connect(mwhConfig)
	if err != nil {
		log.Fatal(err)
	}
	defer mwhConnection.Close()

	// 3. Initialize database transactions, so that we can rollback if disaster strikes.
	phxTxn := phoenixConnection.Begin()
	mwhTxn := mwhConnection.Begin()

	// 4. Get the list of all product variants in the system.
	var variants []*productVariant
	if err := phoenixConnection.Find(&variants).Error; err != nil {
		log.Fatal(err)
	}

	// 5. Iterate through all variants and ensure that the SKU is migrated to MWH.
	for _, variant := range variants {
		fmt.Printf("Migrating variant with code: %s, form ID: %d, and scope: %s\n", variant.Code, variant.FormID, variant.Scope)

		// 6. Check to see if a mapping already exists. If it does, skip.
		mappings := []*productVariantSku{}
		if err := phxTxn.Where("variant_form_id = ?", variant.FormID).Find(&mappings).Error; err != nil {
			phxTxn.Rollback()
			mwhTxn.Rollback()
			log.Fatal(err)
		}

		if len(mappings) > 0 {
			continue
		}

		// 7. The basic template for the mapping.
		mapping := productVariantSku{SkuCode: variant.Code, VariantFormID: variant.FormID}

		// 8. Check to see if a SKU with the desired code exists.
		skus := []*models.SKU{}
		if err := mwhTxn.Where("code = ?", variant.Code).Find(&skus).Error; err != nil {
			phxTxn.Rollback()
			mwhTxn.Rollback()
			log.Fatal(err)
		}

		if len(skus) > 1 {
			phxTxn.Rollback()
			mwhTxn.Rollback()
			log.Fatal(fmt.Errorf("Found %d entries for SKU code %s", len(skus), variant.Code))
		} else if len(skus) == 1 {
			mapping.SkuID = int(skus[0].ID)
		} else {
			// 8a. Create a SKU
			sku := models.SKU{
				Scope:            variant.Scope,
				Code:             variant.Code,
				RequiresShipping: true,
				ShippingClass:    "default",
			}

			if err := mwhTxn.Create(&sku).Error; err != nil {
				phxTxn.Rollback()
				mwhTxn.Rollback()
				log.Fatal(err)
			}

			mapping.SkuID = int(sku.ID)
		}

		// 9. Create the mapping.
		if err := phxTxn.Create(&mapping).Error; err != nil {
			phxTxn.Rollback()
			mwhTxn.Rollback()
			log.Fatal(err)
		}

		// 10. Commit the DB transaction
		if err := mwhTxn.Commit().Error; err != nil {
			phxTxn.Rollback()
			log.Fatal(err)
		}

		if err := phxTxn.Commit().Error; err != nil {
			log.Fatal(err)
		}
	}
}
