package services

import responses.{GiftCardResponse, TheResponse}

package object giftcards {
  type BulkGiftCardUpdateResponse = TheResponse[Seq[GiftCardResponse.RootSimple]]
}