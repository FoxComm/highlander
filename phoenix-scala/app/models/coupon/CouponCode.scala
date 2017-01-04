package models.coupon

import java.time.Instant

import scala.annotation.tailrec
import scala.util.Random

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
  * A coupon code is a way to reference a coupon from the outside world.
  * Multiple codes may point to the same coupon.
  */
case class CouponCode(id: Int = 0,
                      code: String,
                      couponFormId: Int,
                      createdAt: Instant = Instant.now)
    extends FoxModel[CouponCode]

class CouponCodes(tag: Tag) extends FoxTable[CouponCode](tag, "coupon_codes") {

  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def code         = column[String]("code")
  def couponFormId = column[Int]("coupon_form_id")
  def createdAt    = column[Instant]("created_at")

  def * = (id, code, couponFormId, createdAt) <> ((CouponCode.apply _).tupled, CouponCode.unapply)
}

object CouponCodes
    extends FoxTableQuery[CouponCode, CouponCodes](new CouponCodes(_))
    with ReturningIdAndString[CouponCode, CouponCodes]
    with SearchByCode[CouponCode, CouponCodes] {

  override val returningQuery = map { s ⇒
    (s.id, s.code)
  }

  def findByCode(code: String): QuerySeq =
    filter(_.code.toLowerCase === code.toLowerCase)

  def findOneByCode(code: String): DBIO[Option[CouponCode]] =
    findByCode(code).one

  def charactersGivenQuantity(quantity: Int): Int = Math.ceil(Math.log10(quantity.toDouble)).toInt

  def isCharacterLimitValid(prefixSize: Int, quantity: Int, requestedLimit: Int): Boolean = {
    val minSuffixSize = charactersGivenQuantity(quantity)
    requestedLimit >= prefixSize + minSuffixSize
  }

  def generateCodes(prefix: String Refined NonEmpty,
                    codeCharacterLength: Int Refined Positive,
                    quantity: Int Refined Positive,
                    attempt: Int = 0): Seq[String] = {

    // FIXME
//    val minNumericLength = charactersGivenQuantity(quantity.value)
//    require(codeCharacterLength >= prefix.length + minNumericLength)

    val numericLength = codeCharacterLength.value - prefix.value.length
    val largestNum    = Math.pow(10, numericLength.value.toDouble).toInt

    generateCodesRecursively(prefix = prefix,
                             codeCharacterLength = codeCharacterLength,
                             quantity = quantity,
                             numericLength = numericLength,
                             largestNum = largestNum)
  }

  /**
    * recursively generates bunches of codes for given params
    * bunches are collected in accumulator sequence
    * if accumulator size is equal or greater than desired - trims extra codes from the end of seq and return it
    * if accumulator size is less than desired - this function is called again
    *
    * @param prefix - code prefix, character part of code
    * @param codeCharacterLength - length of character part of code
    * @param quantity - number of codes to generate
    * @param numericLength - length of numeric part of code
    * @param largestNum - largest number that can be generated for given length of it
    * @return - list of generated codes for given params
    */
  private def generateCodesRecursively(prefix: String,
                                       codeCharacterLength: Int Refined Positive,
                                       quantity: Int Refined Positive,
                                       numericLength: Int,
                                       largestNum: Int): Seq[String] = {

    @tailrec
    def collectCodes(codesAcc: Seq[String] = Seq.empty): Seq[String] = {
      val codes = (1 to quantity.value).map { i ⇒
        generateCode(prefix = prefix,
                     number = Random.nextInt(largestNum),
                     largestNum = largestNum,
                     numericLength = numericLength.value)
      }.distinct

      val resultSet = codesAcc.union(codes).distinct

      if (resultSet.length >= quantity.value) {
        resultSet.take(quantity.value)
      } else {
        collectCodes(resultSet)
      }
    }

    collectCodes()
  }

  private def generateCode(prefix: String,
                           number: Int,
                           largestNum: Int,
                           numericLength: Int): String = {
    require(numericLength >= 0)
    require(largestNum >= 0)
    require(number <= largestNum)
    val num = s"%0${numericLength}d".format(number)
    s"${prefix}${num}"
  }

  private val rootLens                               = lens[CouponCode]
  val returningLens: Lens[CouponCode, (Int, String)] = rootLens.id ~ rootLens.code
}
