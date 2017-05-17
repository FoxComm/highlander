package services

import cats.implicits._
import org.json4s.JsonAST._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.PrivateMethodTester
import org.scalatest.prop.PropertyChecks
import scala.collection.JavaConverters._
import testutils.TestBase

class EntityExporterTest extends TestBase with PropertyChecks with PrivateMethodTester {
  val fieldsGen: Gen[List[String]] =
    Gen.listOf(Gen.alphaStr.suchThat(s ⇒ !s.contains(".") && s.nonEmpty))

  val arrayIdxGen: Gen[String] = Arbitrary.arbitrary[Int].map(idx ⇒ s"[$idx]")

  val fullFieldsGen: Gen[List[(String, Option[String])]] = fieldsGen
    .flatMap(fs ⇒ Gen.sequence(fs.map(f ⇒ Gen.option(arrayIdxGen).map(i ⇒ f → i))))
    .map(_.asScala.toList)

  "EntityExporter" - {
    val getPath = {
      val method = PrivateMethod[List[String]]('getPath)
      (field: String, removeArrayIndices: Boolean) ⇒
        EntityExporter invokePrivate method(field, removeArrayIndices)
    }

    val extractValue = {
      val method = PrivateMethod[Option[String]]('extractValue)
      (path: List[String], acc: Option[JValue]) ⇒
        EntityExporter invokePrivate method(path, acc)
    }

    "succeeds to get full path from field" in {
      forAll(fieldsGen) { fields ⇒
        getPath(fields.mkString("."), false) must === (fields)
      }

      forAll(fullFieldsGen) { fields ⇒
        getPath(fields.map { case (f, i) ⇒ f + i.getOrElse("") }.mkString("."), true) must === (
            fields.map(_._1))
      }
    }

    "succeeds to extract value from json under given path" in {
      extractValue(
          getPath("whatever.arr[1]", false),
          JObject("whatever" → JObject("arr" → JArray(JNull :: JInt(42) :: Nil))).some).value must === (
          "42")

      extractValue(getPath("whatever", false),
                   JObject("whatever" → JString("""text with '"' """)).some).value must === (
          """"text with '""' """")

      extractValue(
          getPath("whatever[-1].x", false),
          JObject("whatever" → JArray(JObject("x" → JBool(true)) :: Nil)).some).value must === (
          "true")
    }

    "returns None for extracted value if path does not exist in json" in {
      extractValue(List("whatever"), JNothing.some) mustBe 'empty
      extractValue(List("whatever"), JNull.some) mustBe 'empty
      extractValue(List("whatever"), JObject("a" → JString("b")).some) mustBe 'empty
    }

    "returns None for extracted value if it's not a simple type" in {
      extractValue(List("whatever"), JObject("whatever" → JArray(JInt(42) :: Nil)).some) mustBe 'empty
      extractValue(List("whatever"), JObject("whatever" → JObject("a" → JInt(42))).some) mustBe 'empty
    }

    "returns None for extracted value if it's null" in {
      extractValue(List("whatever"), JObject("whatever" → JNull).some) mustBe 'empty
    }

    "returns None for extracted value if array index is out of range" in {
      extractValue(getPath("whatever[1]", false),
                   JObject("whatever" → JArray(JInt(42) :: Nil)).some) mustBe 'empty
    }
  }
}
