package services.customerGroups

import models.account.Scope
import models.customer._
import responses.GroupResponses.GroupTemplateResponse._
import utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._

object GroupTemplateManager {

  def getAll()(implicit ec: EC, db: DB, au: AU): DbResultT[Seq[Root]] =
    for {
      scope         ← * <~ Scope.current
      usedTemplates ← * <~ GroupTemplateInstances.findByScope(scope).result
      usedTemplateIds = usedTemplates.map(_.groupTemplateId)
      templates ← * <~ CustomerGroupTemplates.result
    } yield templates.filterNot(t ⇒ usedTemplateIds.contains(t.id)).map(build(_))

}
