package testutils

import cats.data.Validated
import cats.data.Validated.Valid
import cats.data.Xor.{left, right}

class CatsHelpersTest extends TestBase with CatsHelpers {
  "rightValue" - {
    "on Right" - {
      "returns the value" in {
        rightValue(right(42L)) must === (42L)
      }
    }

    "on Left" - {
      "throws an exception with a good error message" in {
        val exception =
          the[RuntimeException] thrownBy {
            rightValue(left[String, Option[Long]]("Invalid number")) must === (Some(42L))
          }

        exception.getMessage must (include("Expected Right") and
        include("got Left") and
        include("Invalid number") and
        include("Option[Long]") and
        include("String"))
      }
    }
  }

  "leftValue" - {
    "on Right" - {
      "throws an exception with a good error message" in {
        val exception =
          the[RuntimeException] thrownBy {
            leftValue(right[String, Option[Long]](Some(42L))) must === ("Invalid number")
          }

        exception.getMessage must (include("Expected Left") and
        include("got Right") and
        include("Some(42)") and
        include("Option[Long]") and
        include("String"))
      }
    }

    "on Left" - {
      "returns the value" in {
        leftValue(left("Invalid number")) must === ("Invalid number")
      }
    }
  }

  "validValue" - {
    "on Valid" - {
      val example = Valid(42L)

      "returns the value" in {
        validValue(example) must === (42L)
      }
    }

    "on Invalid" - {
      val example = Validated.invalid[String, Option[Long]]("That is not a number!")

      "throws an exception with a good error message" in {
        val exception =
          the[RuntimeException] thrownBy {
            validValue(example) must === (Some(42L))
          }

        exception.getMessage must (include("Expected Valid") and
        include("got Invalid") and
        include("That is not a number!") and
        include("String"))
      }
    }
  }

  "invalidValue" - {
    "on Valid" - {
      val example = Validated.valid[String, Option[Long]](Some(42))

      "throws an exception with a good error message" in {
        val exception =
          the[RuntimeException] thrownBy {
            invalidValue(example) must === ("foo")
          }

        exception.getMessage must (include("Expected Invalid") and
        include("got Valid") and
        include("Some(42)") and
        include("String"))
      }
    }

    "on Invalid" - {
      val example = Validated.invalid[String, Option[Long]]("That is not a number!")

      "returns the value" in {
        invalidValue(example) must === ("That is not a number!")
      }
    }
  }

  "implicits" - {
    "right" in {
      right("left").rightVal must === ("left")
    }

    "left" in {
      left("right").leftVal must === ("right")
    }

    "valid" in {
      Validated.valid("totally valid").validVal must === ("totally valid")
    }

    "invalid" in {
      Validated.invalid("seems fishy").invalidVal must === ("seems fishy")
    }
  }
}
