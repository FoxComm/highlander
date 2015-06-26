package object validators {
  def notExpired[T](year: Int, month: Int) = CreditCard.notExpired[T](year, month)

  def withinTwentyYears[T](year: Int, month: Int) = CreditCard.withinTwentyYears[T](year, month)
}
