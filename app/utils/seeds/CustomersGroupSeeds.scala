package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.{CustomerDynamicGroup, CustomerDynamicGroups}
import utils.DbResultT._
import utils.DbResultT.implicits._
import org.json4s.JObject

trait CustomersGroupSeeds {

  type Groups = (CustomerDynamicGroup#Id, CustomerDynamicGroup#Id)

  def fakeJson = JObject()

  def createGroups: DbResultT[Groups] = for {
    groups ← * <~ CustomerDynamicGroups.createAllReturningIds(groups)
  } yield groups.toList match {
    case c1 :: c2 :: Nil ⇒ (c1, c2)
    case _ ⇒ ???
  }

  def group1 = CustomerDynamicGroup(name = "Super awesome group",
    clientState = fakeJson, createdBy = 1,
    elasticRequest = fakeJson, customersCount = Some(500))

  def group2 = CustomerDynamicGroup(name = "Top 10%",
    clientState = fakeJson, createdBy = 1,
    elasticRequest = fakeJson, customersCount = Some(200))

  def groups: Seq[CustomerDynamicGroup] = Seq(group1, group2)

  def group: CustomerDynamicGroup = group1

}
