package services.activity

import models.Note
import models.cord.Cart
import models.coupon.CouponCode
import models.payment.PaymentMethod
import models.shipping.ShippingMethod
import responses.cord.CartResponse
import responses.{AddressResponse, CreditCardsResponse, GiftCardResponse, UserResponse}

object CartTailored {

  case class CartCreated(admin: Option[UserResponse.Root], cart: CartResponse)
      extends ActivityBase[CartCreated]

  /* Cart Line Items */
  case class CartLineItemsAddedGiftCard(admin: UserResponse.Root,
                                        cart: CartResponse,
                                        giftCard: GiftCardResponse.Root)
      extends ActivityBase[CartLineItemsAddedGiftCard]

  case class CartLineItemsUpdatedGiftCard(admin: UserResponse.Root,
                                          cart: CartResponse,
                                          giftCard: GiftCardResponse.Root)
      extends ActivityBase[CartLineItemsUpdatedGiftCard]

  case class CartLineItemsDeletedGiftCard(admin: UserResponse.Root,
                                          cart: CartResponse,
                                          giftCard: GiftCardResponse.Root)
      extends ActivityBase[CartLineItemsDeletedGiftCard]

  case class CartLineItemsUpdatedQuantities(cart: CartResponse,
                                            oldQuantities: Map[String, Int],
                                            newQuantities: Map[String, Int],
                                            admin: Option[UserResponse.Root])
      extends ActivityBase[CartLineItemsUpdatedQuantities]

  case class CartLineItemsRemoved(carts: Seq[String],
                                  skuIds: Seq[Int],
                                  admin: Option[UserResponse.Root])
      extends ActivityBase[CartLineItemsRemoved]

  /* Cart Shipping Methods */
  case class CartShippingMethodUpdated(cart: CartResponse,
                                       shippingMethod: Option[ShippingMethod],
                                       admin: Option[UserResponse.Root])
      extends ActivityBase[CartShippingMethodUpdated]

  case class CartShippingMethodRemoved(cart: CartResponse,
                                       shippingMethod: ShippingMethod,
                                       admin: Option[UserResponse.Root])
      extends ActivityBase[CartShippingMethodRemoved]

  /* Cart Shipping Addresses */
  case class CartShippingAddressAdded(cart: CartResponse,
                                      shippingAddress: AddressResponse,
                                      admin: Option[UserResponse.Root])
      extends ActivityBase[CartShippingAddressAdded]

  case class CartShippingAddressUpdated(cart: CartResponse,
                                        shippingAddress: AddressResponse,
                                        admin: Option[UserResponse.Root])
      extends ActivityBase[CartShippingAddressUpdated]

  case class CartShippingAddressRemoved(cart: CartResponse,
                                        shippingAddress: AddressResponse,
                                        admin: Option[UserResponse.Root])
      extends ActivityBase[CartShippingAddressRemoved]

  /* Cart Payment Methods */
  case class CartPaymentMethodAddedCreditCard(cart: CartResponse,
                                              creditCard: CreditCardsResponse.Root,
                                              admin: Option[UserResponse.Root])
      extends ActivityBase[CartPaymentMethodAddedCreditCard]

  case class CartPaymentMethodAddedGiftCard(cart: CartResponse,
                                            giftCard: GiftCardResponse.Root,
                                            amount: Int,
                                            admin: Option[UserResponse.Root])
      extends ActivityBase[CartPaymentMethodAddedGiftCard]

  case class CartPaymentMethodUpdatedGiftCard(cart: CartResponse,
                                              giftCard: GiftCardResponse.Root,
                                              oldAmount: Option[Int],
                                              amount: Int,
                                              admin: Option[UserResponse.Root])
      extends ActivityBase[CartPaymentMethodUpdatedGiftCard]

  case class CartPaymentMethodAddedStoreCredit(cart: CartResponse,
                                               amount: Int,
                                               admin: Option[UserResponse.Root])
      extends ActivityBase[CartPaymentMethodAddedStoreCredit]

  case class CartPaymentMethodDeleted(cart: CartResponse,
                                      pmt: PaymentMethod.Type,
                                      admin: Option[UserResponse.Root])
      extends ActivityBase[CartPaymentMethodDeleted]

  case class CartPaymentMethodDeletedGiftCard(cart: CartResponse,
                                              giftCard: GiftCardResponse.Root,
                                              admin: Option[UserResponse.Root])
      extends ActivityBase[CartPaymentMethodDeletedGiftCard]

  /* Cart Coupons */
  case class CartCouponAttached(cart: Cart, couponCode: CouponCode)
      extends ActivityBase[CartCouponAttached]

  case class CartCouponDetached(cart: Cart) extends ActivityBase[CartCouponDetached]

  /* Cart Notes */
  case class CartNoteCreated(admin: UserResponse.Root, cart: Cart, note: Note)
      extends ActivityBase[CartNoteCreated]

  case class CartNoteUpdated(admin: UserResponse.Root, cart: Cart, oldNote: Note, note: Note)
      extends ActivityBase[CartNoteUpdated]

  case class CartNoteDeleted(admin: UserResponse.Root, cart: Cart, note: Note)
      extends ActivityBase[CartNoteDeleted]
}
