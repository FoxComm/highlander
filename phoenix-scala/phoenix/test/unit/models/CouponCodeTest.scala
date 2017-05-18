package models

import scala.util.Random

import models.coupon._
import testutils.TestBase

class CouponCodeTest extends TestBase {

  "CouponCodes" - {
    ".generateCodes" - {
      "generates enough codes" in {
        val codePrefix      = "BASE"
        val desiredQuantity = 10
        val result          = CouponCodes.generateCodes(codePrefix, 5, desiredQuantity)

        result.length must === (desiredQuantity)
        result.toSet.size must === (result.length)

        val desiredQuantity2 = 100
        val result2          = CouponCodes.generateCodes(codePrefix, 6, desiredQuantity2)

        result2.length must === (desiredQuantity2)
        result2.toSet.size must === (result2.length)
      }

      "generates correct number of codes" in {
        val codePrefix      = "RAND"
        val desiredQuantity = 1 + Random.nextInt(1000 - 1)
        val result          = CouponCodes.generateCodes(codePrefix, 10, desiredQuantity)

        result.length must === (desiredQuantity)
        result.toSet.size must === (result.length)
      }

      "generates long codes (as in seeds)" in {
        val codePrefix = "LONG"
        val codeLength = 15
        val codesQty   = 10

        val result = CouponCodes.generateCodes(codePrefix, codeLength, codesQty)

        result.length must === (codesQty)
        result.toSet.size must === (result.length)
      }
    }
  }
}
