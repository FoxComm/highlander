package models.objects

import org.json4s.JsonAST.{JNothing, JString, JObject}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import testutils.TestBase

class ObjectUtilsTest extends TestBase with GeneratorDrivenPropertyChecks {

  "ObjectUtils" - {

    ".key" - {
      "returns correct length keys when ‘fields’ are empty" in forAll { (content: String) ⇒
        ObjectUtils
          .key(JString(content), JNothing) must have size ObjectUtils.KEY_LENGTH_HEX.toLong
      }

      "returns correct hash for collisions" in forAll { (content: String, numCollisionsBig: Int) ⇒
        val hash = ObjectUtils.key(JString(content), JNothing)

        val numCollisions = 1 + (numCollisionsBig % 10).abs
        val artificialCollisions = JObject((0 until numCollisions).toList map (i ⇒
                  s"$hash${if (i > 0) s"/$i" else ""}" → JString(s"$content/$i")))

        val hashColl = ObjectUtils.key(JString(content), artificialCollisions)

        hashColl mustBe s"$hash/$numCollisions"
        hashColl.size must be > ObjectUtils.KEY_LENGTH_HEX_WITH_SLASH
      }
    }

  }

}
