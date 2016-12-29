package services

import (
	"errors"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
)

type shippingMethodService struct {
	repository repositories.IShippingMethodRepository
}

type IShippingMethodService interface {
	GetShippingMethods() ([]*models.ShippingMethod, error)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, error)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	DeleteShippingMethod(id uint) error
	EvaluateForOrder(order *payloads.Order) ([]*responses.OrderShippingMethod, error)
}

func NewShippingMethodService(repository repositories.IShippingMethodRepository) IShippingMethodService {
	return &shippingMethodService{repository}
}

func (service *shippingMethodService) GetShippingMethods() ([]*models.ShippingMethod, error) {
	return service.repository.GetShippingMethods()
}

func (service *shippingMethodService) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	return service.repository.GetShippingMethodByID(id)
}

func (service *shippingMethodService) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	return service.repository.CreateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	return service.repository.UpdateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) DeleteShippingMethod(id uint) error {
	return service.repository.DeleteShippingMethod(id)
}

func (service *shippingMethodService) EvaluateForOrder(order *payloads.Order) ([]*responses.OrderShippingMethod, error) {
	methods, err := service.repository.GetShippingMethods()
	if err != nil {
		return nil, err
	}

	resps := []*responses.OrderShippingMethod{}
	for _, method := range methods {
		resp := &responses.OrderShippingMethod{
			IsEnabled:        true,
			ShippingMethodID: method.ID,
			Price: responses.Money{
				Currency: "USD",
				Value:    method.Cost,
			},
		}

		resps = append(resps, resp)
	}
	return resps, nil
}

func evaluateCondition(condition models.Condition, order interface{}) (result bool, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = errors.New("Unable to convert condition payload to order")
		}
	}()

	orderPayload := order.(*payloads.Order)

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

func evaluateOrderCondition(condition models.Condition, order *payloads.Order) (bool, error) {
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

func evaluateShippingAddressCondition(condition models.Condition, order *payloads.Order) (bool, error) {
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
