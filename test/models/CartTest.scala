package models

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{MustMatchers, FreeSpec}

class CartTest extends FreeSpec with TypeCheckedTripleEquals {
  "Cart" - {
    "supports guest checkout if an account is defined" in {
      assert(Cart(1, accountId = None).isGuest    === false)
      assert(Cart(1, accountId = Some(2)).isGuest === true)
    }
  }
}
