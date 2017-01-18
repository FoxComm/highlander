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
        // *2L because KeyLength is in bytes and we’re encoding them with hex codes.
        ObjectUtils.key(JString(content), JNothing) must have size (ObjectUtils.KeyLength * 2L)
      }

      "returns correct hash for collisions" in forAll { (content: String, numCollisionsBig: Int) ⇒
        val hash = ObjectUtils.key(JString(content), JNothing)

        val numCollisions = 1 + (numCollisionsBig % 10).abs
        val artificialCollisions = JObject((0 until numCollisions).toList.map { i ⇒
          val suffix = if (i > 0) s"/$i" else ""
          s"$hash$suffix" → JString(s"$content/$i")
        })

        val hashColl = ObjectUtils.key(JString(content), artificialCollisions)

        hashColl must === (s"$hash/$numCollisions")
      }
    }

    ".createForm" - {
      "creates new forms from scratch" in forAll { (kv: List[(String, String)]) ⇒
        val (keyMap, form) = ObjectUtils.createForm(toJObject(kv))

        keyMap must === (toKeyMap(kv))
        form.asInstanceOf[JObject].obj.toMap must === (toForm(kv))
      }
    }

    ".updateForm" - {
      "updates forms correctly" in forAll {
        (oldHumanForm: List[(String, String)], humanFormUpdate: List[(String, String)]) ⇒
          val (_, oldForm) = ObjectUtils.createForm(toJObject(oldHumanForm))
          val (updatedKeymap, updatedForm) =
            ObjectUtils.updateForm(oldForm, toJObject(humanFormUpdate))

          updatedForm.asInstanceOf[JObject].obj.toMap must === (
            toForm(oldHumanForm) ++ toForm(humanFormUpdate))
          updatedKeymap must === (toKeyMap(humanFormUpdate))
      }
    }

  }

  private def toJObject(kv: List[(String, String)]) =
    JObject(kv.map {
      case (k, v) ⇒ k → JString(v)
    })

  private def toKeyMap(humanForm: List[(String, String)]): Map[String, String] =
    humanForm.map {
      case (k, v) ⇒ k → ObjectUtils.key(JString(v), JNothing)
    }.toMap

  private def toForm(humanForm: List[(String, String)]): Map[String, JString] =
    humanForm.map {
      case (k, v) ⇒ ObjectUtils.key(JString(v), JNothing) → JString(v)
    }.toMap

}
