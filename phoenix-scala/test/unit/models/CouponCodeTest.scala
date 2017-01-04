package models

import eu.timepit.refined._
import scala.util.Random

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import models.coupon._
import testutils.TestBase

class CouponCodeTest extends TestBase {

  "CouponCodes" - {
    ".generateCodes" - {
      "generates enough codes" in {
        val codePrefix      = refineMV[NonEmpty]("BASE")
        val desiredQuantity = refineMV[Positive](10)
        val result          = CouponCodes.generateCodes(codePrefix, refineMV[Positive](5), desiredQuantity)

        result.length must === (desiredQuantity.value)
        result.toSet.size must === (result.length)

        val desiredQuantity2 = refineMV[Positive](100)
        val result2 =
          CouponCodes.generateCodes(codePrefix, refineMV[Positive](6), desiredQuantity2)

        result2.length must === (desiredQuantity2.value)
        result2.toSet.size must === (result2.length)
      }

      "generates correct number of codes" in {
        val codePrefix      = refineMV[NonEmpty]("RAND")
        val desiredQuantity = refineMV[Positive](1000)
        val result          = CouponCodes.generateCodes(codePrefix, refineMV[Positive](10), desiredQuantity)

        result.length must === (desiredQuantity.value)
        result.toSet.size must === (result.length)
      }

      "generates long codes (as in seeds)" in {
        val codePrefix = refineMV[NonEmpty]("LONG")
        val codeLength = refineMV[Positive](15)
        val codesQty   = refineMV[Positive](10)

        val result = CouponCodes.generateCodes(codePrefix, codeLength, codesQty)

        result.length must === (codesQty.value)
        result.toSet.size must === (result.length)
      }
    }
  }
}
