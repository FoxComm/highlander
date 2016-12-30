package models.objects

import org.json4s.JsonAST.{JNothing, JString}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import testutils.TestBase

class ObjectUtilsTest extends TestBase with GeneratorDrivenPropertyChecks {

  "ObjectUtils" - {

    ".key" - {
      "returns correct length keys when ‘fields’ are empty" in forAll { (content: String) ⇒
        ObjectUtils
          .key(JString(content), JNothing) must have size ObjectUtils.KEY_LENGTH_HEX.toLong
      }
    }

    // TODO:
    //    ensuring { key ⇒
    //      if (key contains '/') key.length > KEY_LENGTH_HEX_WITH_SLASH
    //      else key.length == KEY_LENGTH_HEX
    //    }

  }

}
