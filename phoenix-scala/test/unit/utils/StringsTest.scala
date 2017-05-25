package utils

import org.scalacheck._
import org.scalatest.prop.PropertyChecks
import testutils.TestBase
import utils.Strings._

class StringsTest extends TestBase with PropertyChecks {
  "Strings" - {
    "should split the string on uppercase" in {
      val wordGen = Gen.zip(Gen.alphaUpperChar, Gen.alphaLowerStr).map { case (h, t) ⇒ h + t }
      forAll(Gen.nonEmptyListOf(wordGen)) { words ⇒
        val expected = words.mkString(" ")
        val actual   = words.mkString.prettify

        actual must === (expected)
      }
    }

    "should quote the string" in {
      val escapeCharGen = Gen.oneOf('"', '\\')
      val quotedGen     = Gen.zip(escapeCharGen, Gen.alphaStr).map { case (h, t) ⇒ h + t }
      forAll(escapeCharGen, Gen.nonEmptyListOf(quotedGen)) { (escapeChar, quoted) ⇒
        val expected = "\"" + quoted.map { s ⇒
          if (s.startsWith(s"$escapeChar") || s.startsWith("\"")) escapeChar + s
          else s
        }.mkString(" ") + "\""
        val actual = quoted.mkString(" ").quote(escapeChar)

        actual must === (expected)
      }
    }
  }
}
