package models.objects

import io.circe._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import testutils.TestBase
import utils.json.yolo._

class ObjectUtilsTest extends TestBase with GeneratorDrivenPropertyChecks {

  "ObjectUtils" - {

    ".key" - {
      "returns correct length keys when ‘fields’ are empty" in forAll { (content: String) ⇒
        // *2L because KeyLength is in bytes and we’re encoding them with hex codes.
        ObjectUtils.key(Json.fromString(content), Json.obj()) must have size (ObjectUtils.KeyLength * 2L)
      }

      "returns correct hash for collisions" in forAll { (content: String, numCollisionsBig: Int) ⇒
        val hash = ObjectUtils.key(Json.fromString(content), Json.obj())

        val numCollisions = 1 + (numCollisionsBig % 10).abs
        val artificialCollisions = Json.fromFields((0 until numCollisions).toList.map { i ⇒
          val suffix = if (i > 0) s"/$i" else ""
          s"$hash$suffix" → Json.fromString(s"$content/$i")
        })

        val hashColl = ObjectUtils.key(Json.fromString(content), artificialCollisions)

        hashColl must === (s"$hash/$numCollisions")
      }
    }

    ".createForm" - {
      "creates new forms from scratch" in forAll { (kv: List[(String, String)]) ⇒
        val (keyMap, form) = ObjectUtils.createForm(toJsonObject(kv))

        keyMap must === (toKeyMap(kv))
        form.extract[JsonObject].toMap must === (toForm(kv))
      }
    }

    ".updateForm" - {
      "updates forms correctly" in forAll {
        (oldHumanForm: List[(String, String)], humanFormUpdate: List[(String, String)]) ⇒
          val (_, oldForm) = ObjectUtils.createForm(toJsonObject(oldHumanForm))
          val (updatedKeymap, updatedForm) =
            ObjectUtils.updateForm(oldForm, toJsonObject(humanFormUpdate))

          updatedForm.asObject.value.toMap must === (
              toForm(oldHumanForm) ++ toForm(humanFormUpdate))
          updatedKeymap must === (toKeyMap(humanFormUpdate))
      }
    }

  }

  private def toJsonObject(kv: List[(String, String)]) =
    Json.fromFields(kv.map {
      case (k, v) ⇒ k → Json.fromString(v)
    })

  private def toKeyMap(humanForm: List[(String, String)]): Map[String, String] =
    humanForm.map {
      case (k, v) ⇒ k → ObjectUtils.key(Json.fromString(v), Json.obj())
    }.toMap

  private def toForm(humanForm: List[(String, String)]): Map[String, Json] =
    humanForm.map {
      case (k, v) ⇒ ObjectUtils.key(Json.fromString(v), Json.obj()) → Json.fromString(v)
    }.toMap

}
