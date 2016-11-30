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

func NewCreateGiftCardPayload(lineItem OrderLineItem, refNum string) *CreateGiftCardPayload {
	return &CreateGiftCardPayload{
		Balance:        lineItem.Price,
		SenderName:     lineItem.Attributes.GiftCard.SenderName,
		RecipientName:  lineItem.Attributes.GiftCard.RecipientName,
		RecipientEmail: lineItem.Attributes.GiftCard.RecipientEmail,
		Message:        lineItem.Attributes.GiftCard.Message,
		CordRef:        refNum,
	}
}
