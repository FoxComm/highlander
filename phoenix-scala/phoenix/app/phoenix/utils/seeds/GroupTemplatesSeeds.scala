package phoenix.utils.seeds

import core.db._
import org.json4s.JObject
import org.json4s.jackson.JsonMethods.parse
import phoenix.models.customer.{CustomerGroupTemplate, CustomerGroupTemplates}
import phoenix.utils.aliases._

trait GroupTemplatesSeeds {

  def createGroupTemplates(scopeId: Int)(implicit db: DB, ac: AC, ec: EC): DbResultT[Unit] =
    for {
      _ ← CustomerGroupTemplates.create(abandonedCartsTemplate)
    } yield DbResultT.unit

  private def abandonedCartsTemplate() =
    CustomerGroupTemplate(
      name = "Abandoned Carts",
      elasticRequest = parse("""{
              |  "query": {
              |    "bool": {
              |      "filter": [
              |        {
              |          "nested": {
              |            "path": "carts",
              |            "query": {
              |              "bool": {
              |                "filter": {
              |                  "range": {
              |                    "carts.updatedAt": {
              |                      "lte": "now-3d/d"
              |                    }
              |                  }
              |                }
              |              }
              |            }
              |          }
              |        }, {
              |          "term": {
              |            "isGuest": false
              |          }
              |        }
              |      ]
              |    }
              |  }
              |}""".stripMargin),
      clientState = fakeQuery
    )

  type GroupTemplates = (CustomerGroupTemplate#Id, CustomerGroupTemplate#Id)

  private def fakeQuery = JObject()

  def createGroups(scopeId: Int)(implicit db: DB, ac: AC, ec: EC): DbResultT[GroupTemplates] =
    for {
      list ← * <~ CustomerGroupTemplates.createAllReturningIds(templates)
    } yield
      list.toList match {
        case gt1 :: gt2 :: Nil ⇒ (gt1, gt2)
        case _                 ⇒ ???
      }

  private def template_1() =
    CustomerGroupTemplate(name = "Donkies Group", elasticRequest = fakeQuery, clientState = fakeQuery)
  private def template_2() =
    CustomerGroupTemplate(name = "Foxes Group", elasticRequest = fakeQuery, clientState = fakeQuery)

  def templates: Seq[CustomerGroupTemplate] = Seq(template_1, template_2)
  def template: CustomerGroupTemplate       = template_1
}
