package main

import (
	"log"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
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

	carrierUSPS := &models.Carrier{
		Name:             "USPS",
		TrackingTemplate: "http://www.stamps.com/shipstatus/submit/?confirmation=",
	}

	carrierFedEx := &models.Carrier{
		Name:             "FedEx",
		TrackingTemplate: "http://www.fedex.com/Tracking?action=track&tracknumbers=",
	}

	var err error
	carrierUSPS, err = carrierRepo.CreateCarrier(carrierUSPS)
	if err != nil {
		return err
	}

	carrierFedEx, err = carrierRepo.CreateCarrier(carrierFedEx)
	if err != nil {
		return err
	}

	log.Printf("Seeding shipping methods...")
	shippingRepo := repositories.NewShippingMethodRepository(db)

	standardShipping := &models.ShippingMethod{
		CarrierID: carrierUSPS.ID,
		Name:      "Standard shipping",
		Code:      "STANDARD",
	}

	if _, err = shippingRepo.CreateShippingMethod(standardShipping); err != nil {
		return err
	}

	standardShippingFree := &models.ShippingMethod{
		CarrierID: carrierUSPS.ID,
		Name:      "Standard shipping",
		Code:      "STANDARD-FREE",
	}

	if _, err = shippingRepo.CreateShippingMethod(standardShippingFree); err != nil {
		return err
	}

	expressShipping := &models.ShippingMethod{
		CarrierID: carrierFedEx.ID,
		Name:      "2-3 day express",
		Code:      "EXPRESS",
	}

	if _, err = shippingRepo.CreateShippingMethod(expressShipping); err != nil {
		return err
	}

	overnightShipping := &models.ShippingMethod{
		CarrierID: carrierFedEx.ID,
		Name:      "Overnight shipping",
		Code:      "OVERNIGHT",
	}

	if _, err = shippingRepo.CreateShippingMethod(overnightShipping); err != nil {
		return err
	}

	return nil
}
