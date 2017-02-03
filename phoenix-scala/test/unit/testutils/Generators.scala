package testutils

import org.scalacheck.Shapeless._
import org.scalacheck.{Arbitrary, Gen}
import utils.apis.CreateSku

trait Generators {
  val idGen: Gen[Int] = Gen.posNum[Int]

  val createSkuGen: Gen[CreateSku] = implicitly[Arbitrary[CreateSku]].arbitrary
}
