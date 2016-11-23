package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/jinzhu/gorm"
)

func main() {
	db, exception := config.DefaultConnection()
	if exception != nil {
		log.Fatalf("Unable to connect to DB with error %s", exception.ToString())
	}

	log.Printf("Started to seed database...")
	if exception := createShippingMethods(db); exception != nil {
		log.Fatalf("Unable to seed shipping methods with error %s", exception.ToString())
	}

	log.Printf("Seeding complete")
}

func createShippingMethods(db *gorm.DB) exceptions.IException {
	log.Printf("Seeding carriers...")
	carrierRepo := repositories.NewCarrierRepository(db)

	carriers := []*models.Carrier{
		&models.Carrier{
			Name:             "USPS",
			TrackingTemplate: "http://www.stamps.com/shipstatus/submit/?confirmation=",
		},
		&models.Carrier{
			Name:             "FedEx",
			TrackingTemplate: "http://www.fedex.com/Tracking?action=track&tracknumbers=",
		},
	}

	var exception exceptions.IException
	for idx, carrier := range carriers {
		carriers[idx], exception = carrierRepo.CreateCarrier(carrier)
		if exception != nil {
			return exception
		}
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
		},
		&models.ShippingMethod{
			CarrierID:    carriers[0].ID,
			Name:         "Standard shipping",
			Code:         "STANDARD-FREE",
			ShippingType: models.ShippingTypeFlat,
			Cost:         0,
		},
		&models.ShippingMethod{
			CarrierID:    carriers[1].ID,
			Name:         "2-3 day express",
			Code:         "EXPRESS",
			ShippingType: models.ShippingTypeFlat,
			Cost:         1500,
		},
		&models.ShippingMethod{
			CarrierID:    carriers[1].ID,
			Name:         "Overnight shipping",
			Code:         "OVERNIGHT",
			ShippingType: models.ShippingTypeFlat,
			Cost:         3000,
		},
	}

	for _, shippingMethod := range shippingMethods {
		if _, exception = shippingRepo.CreateShippingMethod(shippingMethod); exception != nil {
			return exception
		}
	}

	return nil
}
