package models.rules

import testutils.TestBase
import utils.seeds.Factories

class ConditionTest extends TestBase {
  "Condition" - {
    "#matches(Int, Condition)" - {
      "Equals" - {
        "returns true when values are equal" in {
          val c = Factories.condition.copy(operator = Condition.Equals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(50, c)
          result must === (true)
        }

        "returns false when values are not equal" in {
          val c = Factories.condition.copy(operator = Condition.Equals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(51, c)
          result must === (false)
        }
      }

      "NotEquals" - {
        "returns false when values are equal" in {
          val c = Factories.condition.copy(operator = Condition.NotEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(50, c)
          result must === (false)
        }

        "returns true when values are not equal" in {
          val c = Factories.condition.copy(operator = Condition.NotEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(51, c)
          result must === (true)
        }
      }

      "GreaterThan" - {
        "returns true when value is greater than condition" in {
          val c = Factories.condition.copy(operator = Condition.GreaterThan,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(51, c)
          result must === (true)
        }

        "returns false when value is equal to condition" in {
          val c = Factories.condition.copy(operator = Condition.GreaterThan,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(50, c)
          result must === (false)
        }

        "returns false when value is less than condition" in {
          val c = Factories.condition.copy(operator = Condition.GreaterThan,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(49, c)
          result must === (false)
        }
      }

      "LessThan" - {
        "returns false when value is greater than condition" in {
          val c = Factories.condition.copy(operator = Condition.LessThan,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(51, c)
          result must === (false)
        }

        "returns false when value is equal to condition" in {
          val c = Factories.condition.copy(operator = Condition.LessThan,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(50, c)
          result must === (false)
        }

        "returns true when value is less than condition" in {
          val c = Factories.condition.copy(operator = Condition.LessThan,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(49, c)
          result must === (true)
        }
      }

      "GreaterThanOrEquals" - {
        "returns true when value is greater than condition" in {
          val c = Factories.condition.copy(operator = Condition.GreaterThanOrEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(51, c)
          result must === (true)
        }

        "returns true when value is equal to condition" in {
          val c = Factories.condition.copy(operator = Condition.GreaterThanOrEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(50, c)
          result must === (true)
        }

        "returns false when value is less than condition" in {
          val c = Factories.condition.copy(operator = Condition.GreaterThanOrEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(49, c)
          result must === (false)
        }
      }

      "LessThanOrEquals" - {
        "returns false when value is greater than condition" in {
          val c = Factories.condition.copy(operator = Condition.LessThanOrEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(51, c)
          result must === (false)
        }

        "returns true when value is equal to condition" in {
          val c = Factories.condition.copy(operator = Condition.LessThanOrEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(50, c)
          result must === (true)
        }

        "returns true when value is less than condition" in {
          val c = Factories.condition.copy(operator = Condition.LessThanOrEquals,
                                           valInt = Some(50),
                                           valString = None)
          val result = Condition.matches(49, c)
          result must === (true)
        }
      }
    }

    "#matches(String, Condition)" - {
      "Equals" - {
        "returns true when values are equal" in {
          val c =
            Factories.condition.copy(operator = Condition.Equals, valString = Some("some string"))
          val result = Condition.matches("some string", c)
          result must === (true)
        }

        "returns false when values are not equal" in {
          val c =
            Factories.condition.copy(operator = Condition.Equals, valString = Some("some string"))
          val result = Condition.matches("another string", c)
          result must === (false)
        }
      }

      "NotEquals" - {
        "returns false when values are equal" in {
          val c = Factories.condition.copy(operator = Condition.NotEquals,
                                           valString = Some("some string"))
          val result = Condition.matches("some string", c)
          result must === (false)
        }

        "returns true when values are not equal" in {
          val c = Factories.condition.copy(operator = Condition.NotEquals,
                                           valString = Some("some string"))
          val result = Condition.matches("another string", c)
          result must === (true)
        }
      }

      "Contains" - {
        "returns true with the condition contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.Contains, valString = Some("me str"))
          val result = Condition.matches("some string", c)
          result must === (true)
        }

        "returns false with the condition doesn't contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.Contains, valString = Some("me str"))
          val result = Condition.matches("another string", c)
          result must === (false)
        }
      }

      "NotContains" - {
        "returns false with the condition contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.NotContains, valString = Some("me str"))
          val result = Condition.matches("some string", c)
          result must === (false)
        }

        "returns true with the condition doesn't contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.NotContains, valString = Some("me str"))
          val result = Condition.matches("another string", c)
          result must === (true)
        }
      }
    }

    "#matches(Option[String], Condition)" - {
      "Equals" - {
        "returns true when values are equal" in {
          val c =
            Factories.condition.copy(operator = Condition.Equals, valString = Some("some string"))
          val result = Condition.matches(Some("some string"), c)
          result must === (true)
        }

        "returns false when values are not equal" in {
          val c =
            Factories.condition.copy(operator = Condition.Equals, valString = Some("some string"))
          val result = Condition.matches(Some("another string"), c)
          result must === (false)
        }
      }

      "NotEquals" - {
        "returns false when values are equal" in {
          val c = Factories.condition.copy(operator = Condition.NotEquals,
                                           valString = Some("some string"))
          val result = Condition.matches(Some("some string"), c)
          result must === (false)
        }

        "returns true when values are not equal" in {
          val c = Factories.condition.copy(operator = Condition.NotEquals,
                                           valString = Some("some string"))
          val result = Condition.matches(Some("another string"), c)
          result must === (true)
        }
      }

      "Contains" - {
        "returns true with the condition contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.Contains, valString = Some("me str"))
          val result = Condition.matches(Some("some string"), c)
          result must === (true)
        }

        "returns false with the condition doesn't contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.Contains, valString = Some("me str"))
          val result = Condition.matches(Some("another string"), c)
          result must === (false)
        }
      }

      "NotContains" - {
        "returns false with the condition contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.NotContains, valString = Some("me str"))
          val result = Condition.matches(Some("some string"), c)
          result must === (false)
        }

        "returns true with the condition doesn't contains the value" in {
          val c =
            Factories.condition.copy(operator = Condition.NotContains, valString = Some("me str"))
          val result = Condition.matches(Some("another string"), c)
          result must === (true)
        }
      }
    }
  }
}
