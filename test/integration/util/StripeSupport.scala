package util

object StripeSupport {
  // https://stripe.com/docs/testing#cards
  private [this] val successfulCards = Map(
    "Visa" -> "4242424242424242",
    "Visa (debit)" -> "4000056655665556",
    "MasterCard" -> "5555555555554444",
    "MasterCard (debit)" -> "5200828282828210",
    "MasterCard (prepaid)" -> "5105105105105100",
    "American Express" -> "378282246310005",
    "Discover" -> "6011111111111117",
    "Diners Club" -> "30569309025904",
    "JCB" -> "3530111333300000"
  )

  // https://stripe.com/docs/testing#cards
  private [this] val failureCards = Map(
    "card_declined" -> "4000000000000002",
    "incorrect_number" -> "4242424242424241", // fails the Luhn check
    "expired_card" -> "4000000000000069"
  )

  def successfulCard:       String = this.successfulCards.get("Visa").get

  def declinedCard:         String = this.failureCards.get("card_declined").get

  def failureCard:          String = this.declinedCard

  def incorrectNumberCard:  String = this.failureCards.get("incorrect_number").get
}
