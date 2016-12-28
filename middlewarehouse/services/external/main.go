package main

import (
	"database/sql"
	"fmt"
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services/external/ups"
)

func main() {
	username := os.Getenv("USERNAME")
	password := os.Getenv("PASSWORD")
	accessCode := os.Getenv("ACCESS_CODE")

	shipment := &models.Shipment{
		Address: models.Address{
			Name: "Dayle Mataya",
			Region: models.Region{
				Name: "Michigan",
				Country: models.Country{
					Name: "United States of America",
				},
			},
			City:        "Farmington Hills",
			Zip:         "48331",
			Address1:    "29918 Kenloch Drive",
			PhoneNumber: "2487881082",
		},
		ShipmentLineItems: []models.ShipmentLineItem{
			models.ShipmentLineItem{
				StockItemUnit: models.StockItemUnit{
					StockItem: models.StockItem{
						StockLocation: models.StockLocation{
							Address: &models.Address{
								Name: "FoxCommerce",
								Region: models.Region{
									Name: "Washington",
									Country: models.Country{
										Name: "United States of America",
									},
								},
								City:        "Seattle",
								Zip:         "98109",
								Address1:    "3131 Elliot Ave.",
								Address2:    sql.NullString{Valid: true, String: "Suite 240"},
								PhoneNumber: "2069637392",
							},
						},
					},
				},
			},
		},
	}

	upsAPI := ups.NewAPI(username, password, accessCode, false)
	charge, err := upsAPI.GetRate(shipment)
	if err != nil {
		panic(err)
	}

	fmt.Printf("The estimated charge is %.2f\n", charge)
}
