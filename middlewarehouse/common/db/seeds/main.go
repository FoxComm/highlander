package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

func main() {
	db, err := config.DefaultConnection()
	if err != nil {
		log.Fatalf("Unable to connect to DB with error %s", err.Error())
	}

	log.Printf("Started to seed database...")
	if err := createShippingMethods(db); err != nil {
		log.Fatalf("Unable to seed shipping methods with error %s", err.Error())
	}

	log.Printf("Seeding complete")
}

func createShippingMethods(db *gorm.DB) error {
	log.Printf("Seeding carriers...")
	carrierRepo := repositories.NewCarrierRepository(db)

	carriers := []*models.Carrier{
		&models.Carrier{
			Name:             "USPS",
			TrackingTemplate: "http://www.stamps.com/shipstatus/submit/?confirmation=",
			Scope:            "1.2",
		},
		&models.Carrier{
			Name:             "FedEx",
			TrackingTemplate: "http://www.fedex.com/Tracking?action=track&tracknumbers=",
			Scope:            "1.2",
		},
	}

	for idx, carrier := range carriers {
		if err := carrierRepo.CreateCarrier(carrier); err != nil {
			return err
		}
		carriers[idx] = carrier
	}

	log.Printf("Seeding shipping methods...")
	shippingRepo := repositories.NewShippingMethodRepository(db)

	shippingMethods := []*models.ShippingMethod{
		&models.ShippingMethod{
			CarrierID:    carriers[0].ID,
			Name:         "Standard shipping",
			Code:         "STANDARD",
			ShippingType: models.ShippingTypeFlat,
			Cost:         300,
			Scope:        "1.2",
		},
		&models.ShippingMethod{
			CarrierID:    carriers[0].ID,
			Name:         "Standard shipping",
			Code:         "STANDARD-FREE",
			ShippingType: models.ShippingTypeFlat,
			Cost:         0,
			Scope:        "1.2",
		},
		&models.ShippingMethod{
			CarrierID:    carriers[1].ID,
			Name:         "2-3 day express",
			Code:         "EXPRESS",
			ShippingType: models.ShippingTypeFlat,
			Cost:         1500,
			Scope:        "1.2",
		},
		&models.ShippingMethod{
			CarrierID:    carriers[1].ID,
			Name:         "Overnight shipping",
			Code:         "OVERNIGHT",
			ShippingType: models.ShippingTypeFlat,
			Cost:         3000,
			Scope:        "1.2",
		},
	}

	for _, shippingMethod := range shippingMethods {
		if _, err := shippingRepo.CreateShippingMethod(shippingMethod); err != nil {
			return err
		}
	}

	return nil
}
