package utils.seeds

import models.customer.{CustomerGroupTemplate, CustomerGroupTemplates}
import org.json4s.JObject
import utils.aliases._
import utils.db._

trait GroupTemplatesSeeds {

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
    CustomerGroupTemplate(name = "Donkies Group",
                          elasticRequest = fakeQuery,
                          clientState = fakeQuery)
  private def template_2() =
    CustomerGroupTemplate(name = "Foxes Group",
                          elasticRequest = fakeQuery,
                          clientState = fakeQuery)

  def templates: Seq[CustomerGroupTemplate] = Seq(template_1, template_2)
  def template: CustomerGroupTemplate       = template_1
}
