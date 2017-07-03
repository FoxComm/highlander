package phoenix.services.activity

import phoenix.models.Note
import phoenix.models.cord.Cart
import phoenix.models.coupon.CouponCode
import phoenix.models.payment.PaymentMethod
import phoenix.models.shipping.ShippingMethod
import phoenix.responses.cord.CartResponse
import phoenix.responses.giftcards.GiftCardResponse
import phoenix.responses.users.UserResponse
import phoenix.responses.{AddressResponse, CreditCardResponse}

object CartTailored {

  case class CartCreated(admin: Option[UserResponse], cart: CartResponse) extends ActivityBase[CartCreated]

  /* Cart Line Items */
  case class CartLineItemsAddedGiftCard(admin: UserResponse, cart: CartResponse, giftCard: GiftCardResponse)
      extends ActivityBase[CartLineItemsAddedGiftCard]

  case class CartLineItemsUpdatedGiftCard(admin: UserResponse, cart: CartResponse, giftCard: GiftCardResponse)
      extends ActivityBase[CartLineItemsUpdatedGiftCard]

  case class CartLineItemsDeletedGiftCard(admin: UserResponse, cart: CartResponse, giftCard: GiftCardResponse)
      extends ActivityBase[CartLineItemsDeletedGiftCard]

  case class CartLineItemsUpdatedQuantities(cart: CartResponse,
                                            oldQuantities: Map[String, Int],
                                            newQuantities: Map[String, Int],
                                            admin: Option[UserResponse])
      extends ActivityBase[CartLineItemsUpdatedQuantities]

  /* Cart Shipping Methods */
  case class CartShippingMethodUpdated(cart: CartResponse,
                                       shippingMethod: Option[ShippingMethod],
                                       admin: Option[UserResponse])
      extends ActivityBase[CartShippingMethodUpdated]

  case class CartShippingMethodRemoved(cart: CartResponse,
                                       shippingMethod: ShippingMethod,
                                       admin: Option[UserResponse])
      extends ActivityBase[CartShippingMethodRemoved]

  /* Cart Shipping Addresses */
  case class CartShippingAddressAdded(cart: CartResponse,
                                      shippingAddress: AddressResponse,
                                      admin: Option[UserResponse])
      extends ActivityBase[CartShippingAddressAdded]

  case class CartShippingAddressUpdated(cart: CartResponse,
                                        shippingAddress: AddressResponse,
                                        admin: Option[UserResponse])
      extends ActivityBase[CartShippingAddressUpdated]

  case class CartShippingAddressRemoved(cart: CartResponse,
                                        shippingAddress: AddressResponse,
                                        admin: Option[UserResponse])
      extends ActivityBase[CartShippingAddressRemoved]

  /* Cart Payment Methods */
  case class CartPaymentMethodAddedCreditCard(cart: CartResponse,
                                              creditCard: CreditCardResponse,
                                              admin: Option[UserResponse])
      extends ActivityBase[CartPaymentMethodAddedCreditCard]

  case class CartPaymentMethodAddedGiftCard(cart: CartResponse,
                                            giftCard: GiftCardResponse,
                                            amount: Long,
                                            admin: Option[UserResponse])
      extends ActivityBase[CartPaymentMethodAddedGiftCard]

  case class CartPaymentMethodUpdatedGiftCard(cart: CartResponse,
                                              giftCard: GiftCardResponse,
                                              oldAmount: Option[Long],
                                              amount: Long,
                                              admin: Option[UserResponse])
      extends ActivityBase[CartPaymentMethodUpdatedGiftCard]

  case class CartPaymentMethodAddedStoreCredit(cart: CartResponse, amount: Long, admin: Option[UserResponse])
      extends ActivityBase[CartPaymentMethodAddedStoreCredit]

  case class CartPaymentMethodDeleted(cart: CartResponse,
                                      pmt: PaymentMethod.Type,
                                      admin: Option[UserResponse])
      extends ActivityBase[CartPaymentMethodDeleted]

  case class CartPaymentMethodDeletedGiftCard(cart: CartResponse,
                                              giftCard: GiftCardResponse,
                                              admin: Option[UserResponse])
      extends ActivityBase[CartPaymentMethodDeletedGiftCard]

  /* Cart Coupons */
  case class CartCouponAttached(cart: Cart, couponCode: CouponCode) extends ActivityBase[CartCouponAttached]

  case class CartCouponDetached(cart: Cart) extends ActivityBase[CartCouponDetached]

  /* Cart Notes */
  case class CartNoteCreated(admin: UserResponse, cart: Cart, note: Note)
      extends ActivityBase[CartNoteCreated]

  case class CartNoteUpdated(admin: UserResponse, cart: Cart, oldNote: Note, note: Note)
      extends ActivityBase[CartNoteUpdated]

  case class CartNoteDeleted(admin: UserResponse, cart: Cart, note: Note)
      extends ActivityBase[CartNoteDeleted]
}
