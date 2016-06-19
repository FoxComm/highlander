package main

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/services"
)

func main() {
	im := services.NewInventoryManager()
	fmt.Printf("%v\n", im)
}
