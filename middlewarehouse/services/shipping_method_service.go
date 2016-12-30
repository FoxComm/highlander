package services

import (
	"errors"
	"fmt"
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/models/rules"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/services/external/ups"
)

type shippingMethodService struct {
	shippingMethodRepo repositories.IShippingMethodRepository
	stockItemRepo      repositories.IStockItemRepository
}

type IShippingMethodService interface {
	GetShippingMethods() ([]*models.ShippingMethod, error)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, error)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	DeleteShippingMethod(id uint) error
	EvaluateForOrder(order *payloads.Order) ([]*responses.OrderShippingMethod, error)
}

func NewShippingMethodService(shippingMethodRepo repositories.IShippingMethodRepository, stockItemRepo repositories.IStockItemRepository) IShippingMethodService {
	return &shippingMethodService{shippingMethodRepo, stockItemRepo}
}

func (service *shippingMethodService) GetShippingMethods() ([]*models.ShippingMethod, error) {
	return service.shippingMethodRepo.GetShippingMethods()
}

func (service *shippingMethodService) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	return service.shippingMethodRepo.GetShippingMethodByID(id)
}

func (service *shippingMethodService) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	return service.shippingMethodRepo.CreateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	return service.shippingMethodRepo.UpdateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) DeleteShippingMethod(id uint) error {
	return service.shippingMethodRepo.DeleteShippingMethod(id)
}

func (service *shippingMethodService) EvaluateForOrder(order *payloads.Order) ([]*responses.OrderShippingMethod, error) {
	// Get all of the shipping methods associated with this order.
	// Then, evaluate each one to see if it is applicable.
	// Finally, we'll return the ones that match.
	//
	// TODO:
	//	1. Scope the shipping methods by the SKU in the order
	//	2. Potentially run through methods in parallel
	methods, err := service.shippingMethodRepo.GetShippingMethods()
	if err != nil {
		return nil, err
	}

	resps := []*responses.OrderShippingMethod{}
	for _, method := range methods {
		var isVisible bool
		var err error

		if method.Conditions.IsEmpty() {
			isVisible = true
		} else {
			isVisible, err = method.Conditions.Evaluate(order, evaluateCondition)
			if err != nil {
				return nil, err
			}
		}

		if isVisible {
			var isDisabled bool
			if method.Restrictions.IsEmpty() {
				isDisabled = false
			} else {
				isDisabled, err = method.Restrictions.Evaluate(order, evaluateCondition)
				if err != nil {
					return nil, err
				}
			}

			var price uint
			if method.ShippingType == models.ShippingTypeVariable {
				// Placeholder for making the call to the external API
				shipment := models.NewShipmentFromOrderPayload(order)
				username := os.Getenv("UPS_USERNAME")
				password := os.Getenv("UPS_PASSWORD")
				token := os.Getenv("UPS_ACCESS_TOKEN")

				// Get the address for the first stock location in the shipment.
				if len(shipment.ShipmentLineItems) > 0 {
					lineItem := shipment.ShipmentLineItems[0]
					item, err := service.stockItemRepo.GetStockItemBySKU(lineItem.SKU)
					if err != nil {
						return nil, err
					}

					address := item.StockLocation.Address
					shipment.ShipmentLineItems[0].StockItemUnit.StockItem.StockLocation.Address = address
				}

				upsAPI := ups.NewAPI(username, password, token, false)
				charge, err := upsAPI.GetRate(shipment)
				if err != nil {
					return nil, err
				}

				price = uint(charge * 100)
			} else {
				price = method.Price
			}

			resp := &responses.OrderShippingMethod{
				IsEnabled:        !isDisabled,
				ShippingMethodID: method.ID,
				Price: responses.Money{
					Currency: "USD",
					Value:    price,
				},
			}

			resps = append(resps, resp)
		}
	}

	fmt.Printf("The resps are %v\n", resps)
	return resps, nil
}

func evaluateCondition(condition rules.Condition, order interface{}) (result bool, err error) {
	orderPayload, ok := order.(*payloads.Order)
	if !ok {
		err = errors.New("Unable to convert condition payload to order")
		return
	}

	switch condition.RootObject {
	case "Order":
		result, err = evaluateOrderCondition(condition, orderPayload)
	case "ShippingAddress":
		result, err = evaluateShippingAddressCondition(condition, orderPayload)
	default:
		err = fmt.Errorf("Invalid shipping condition root object %s", condition.RootObject)
	}

	return
}

func evaluateOrderCondition(condition rules.Condition, order *payloads.Order) (bool, error) {
	var result bool
	var err error

	switch condition.Field {
	case "subtotal":
		result, err = condition.MatchesInt(order.Totals.SubTotal)
	case "grandTotal":
		result, err = condition.MatchesInt(order.Totals.Total)
	default:
		err = fmt.Errorf("Invalid order condition field %s", condition.Field)
	}

	return result, err
}

func evaluateShippingAddressCondition(condition rules.Condition, order *payloads.Order) (bool, error) {
	var result bool
	var err error

	if order.ShippingAddress == nil {
		return false, nil
	}

	shippingAddress := order.ShippingAddress
	switch condition.Field {
	case "address1":
		result, err = condition.MatchesString(shippingAddress.Address1)
	case "address2":
		address2 := ""
		if shippingAddress.Address2 != nil {
			address2 = *shippingAddress.Address2
		}
		result, err = condition.MatchesString(address2)
	case "city":
		result, err = condition.MatchesString(shippingAddress.City)
	case "regionId":
		result, err = condition.MatchesUint(shippingAddress.Region.ID)
	case "countryId":
		result, err = condition.MatchesUint(shippingAddress.Region.CountryID)
	case "regionName":
		result, err = condition.MatchesString(shippingAddress.Region.Name)
	case "regionAbbrev":
		abbrev, err := utils.GetShortRegion(shippingAddress.Region.Name)
		if err == nil {
			result, err = condition.MatchesString(abbrev)
		}
	case "zip":
		result, err = condition.MatchesString(shippingAddress.Zip)
	default:
		err = fmt.Errorf("Invalid shipping address condition field %s", condition.Field)
	}

	return result, err
}
