package payloads

// Giftcard represent the properties to send a giftcard
type GiftCard struct {
	Code           *string `json:"code"`
	SenderName     string  `json:"senderName" binding:"required"`
	RecipientName  string  `json:"recipientName" binding:"required"`
	RecipientEmail string  `json:"recipientEmail" binding:"required"`
	Message        string  `json:"message"`
}

type CreateGiftCardPayload struct {
	Scope	       *string `json:"scope"`
	Balance        uint   `json:"balance"`
	SenderName     string `json:"senderName" binding:"required"`
	RecipientName  string `json:"recipientName" binding:"required"`
	RecipientEmail string `json:"recipientEmail" binding:"required"`
	Message        string `json:"message"`
	CordRef        string `json:"cordRef"`
}

type GiftCardResponse struct {
	Code string `json:"code" binding:"required"`
}

func NewCreateGiftCardPayload(lineItem OrderLineItem, refNum string, scope string) *CreateGiftCardPayload {
	return &CreateGiftCardPayload{
		Scope: 		&scope,
		Balance:        lineItem.Price,
		SenderName:     lineItem.Attributes.GiftCard.SenderName,
		RecipientName:  lineItem.Attributes.GiftCard.RecipientName,
		RecipientEmail: lineItem.Attributes.GiftCard.RecipientEmail,
		Message:        lineItem.Attributes.GiftCard.Message,
		CordRef:        refNum,
	}
}
