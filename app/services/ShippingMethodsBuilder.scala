package services

abstract class ShippingMethodsBuilder {

  def availableShippingMethods(): Unit
  // Which shipping methods are active right now?

  def calculateShippingMethodPrice(): Unit
  // What is the price of a certain shipping method based on the current order details?

}
