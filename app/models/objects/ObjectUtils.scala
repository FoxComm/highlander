package models.objects

import services.Failure
import services.IlluminateFailure._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}
import java.security.MessageDigest

object ObjectUtils { 


  def key(content: JValue) : String = {
    val md = java.security.MessageDigest.getInstance("SHA-1");
    md.digest(compact(render(content)).getBytes).slice(0, 7).toString
  }

  def key(content: String) : String = key(JString(content)) 

  def attribute(content: JValue) : JField = {
    (key(content), content)
  }

  def attributes(values: Seq[JValue]) : JValue = 
    JObject((values map attribute).toList)

  type KeyMap = Map[String, String]
  def createForm(form: JValue) : (KeyMap, JValue) = {
    form match { 
      case JObject(o) ⇒  {
        val m = o.obj.map {
          case (attr, value) ⇒  { 
            val k = key(value)
            (Map(attr → k), (k, value))
          }
        }
        val keyMap = m.map{_._1}.reduce(_++_)
        val newForm = JObject(m.map{_._2}.toList)
        (keyMap, newForm) 
      }
      case _ ⇒  (Map(), JNothing)
    }
  }

  def updateForm(oldForm: JValue, updatedForm: JValue) : (KeyMap, JValue) = {
    val (keyMap, newForm) = createForm(updatedForm)
    (keyMap, oldForm merge newForm)
  }

  def newShadow(oldShadow: JValue, keyMap: KeyMap) : JValue = {
    oldShadow match { 
      case JObject(o) ⇒ {
        o.obj.map { 
          case (key, value) ⇒  { 
            val t = value \ "type"
            val ref = value \ "ref" match {
              case JString(s) ⇒ s
              case _ ⇒  key
            }
            (key, ( "type" → t) ~ ( "ref" → keyMap.getOrElse(ref, key))) 
          }
        }
      }
      case _ ⇒ JNothing
    }
  }
}

