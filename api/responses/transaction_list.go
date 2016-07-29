package responses

type TransactionList struct {
	CreditCards  []CreditCardTransaction  `json:"creditCards"`
	GiftCards    []GiftCardTransaction    `json:"giftCards"`
	StoreCredits []StoreCreditTransaction `json:"storeCredits"`
}
