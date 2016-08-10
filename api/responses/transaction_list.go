package responses

import (
	"encoding/json"
	"strings"

	"github.com/FoxComm/middlewarehouse/models"
)

type TransactionList struct {
	CreditCards  []CreditCardTransaction  `json:"creditCards"`
	GiftCards    []GiftCardTransaction    `json:"giftCards"`
	StoreCredits []StoreCreditTransaction `json:"storeCredits"`
}

func NewTransactionListFromModelsList(transactions []*models.ShipmentTransaction) *TransactionList {
	transactionList := &TransactionList{}

	for _, model := range transactions {
		switch model.Type {
		case models.TransactionCreditCard:
			transactionList.CreditCards = append(transactionList.CreditCards, *newCreditCardTransactionFromModel(model))
			break
		case models.TransactionGiftCard:
			transactionList.GiftCards = append(transactionList.GiftCards, *newGiftCardTransactionFromModel(model))
			break
		case models.TransactionStoreCredit:
			transactionList.StoreCredits = append(transactionList.StoreCredits, *newStoreCreditTransactionFromModel(model))
			break
		}
	}

	return transactionList
}

func newCreditCardTransactionFromModel(model *models.ShipmentTransaction) *CreditCardTransaction {
	transaction := &CreditCardTransaction{
		ID:        model.ID,
		Amount:    model.Amount,
		CreatedAt: model.CreatedAt.String(),
	}

	if err := json.NewDecoder(strings.NewReader(model.Source)).Decode(transaction); err != nil {
		panic(err)
	}

	return transaction
}

func newGiftCardTransactionFromModel(model *models.ShipmentTransaction) *GiftCardTransaction {
	transaction := &GiftCardTransaction{
		ID:        model.ID,
		Amount:    model.Amount,
		CreatedAt: model.CreatedAt.String(),
	}

	if err := json.NewDecoder(strings.NewReader(model.Source)).Decode(transaction); err != nil {
		panic(err)
	}

	return transaction
}

func newStoreCreditTransactionFromModel(model *models.ShipmentTransaction) *StoreCreditTransaction {
	transaction := &StoreCreditTransaction{
		ID:        model.ID,
		Amount:    model.Amount,
		CreatedAt: model.CreatedAt.String(),
	}

	if err := json.NewDecoder(strings.NewReader(model.Source)).Decode(transaction); err != nil {
		panic(err)
	}

	return transaction
}
