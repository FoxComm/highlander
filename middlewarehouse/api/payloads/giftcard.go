package payloads

// Giftcard represent the properties to send a giftcard
type GiftCard struct {
	SenderName     string `json:"senderName" binding:"required"`
	RecipientName  string `json:"recipientName" binding:"required"`
	RecipientEmail string `json:"recipientEmail" binding:"required"`
	Message        string `json:"message"`
}

type CreateGiftCardPayload struct {
	Balance uint     `json:"balance"`
	Details GiftCard `json:"details"`
	CordRef string   `json:"cordRef"`
}
