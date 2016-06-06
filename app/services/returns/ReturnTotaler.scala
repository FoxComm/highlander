package services.returns

import cats.implicits._
import models.returns._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object ReturnTotaler {
  def subTotal(rma: Return)(implicit ec: EC): DBIO[Option[Int]] =
    sql"""select count(*), sum(coalesce(gc.original_balance, 0)) + sum(coalesce(cast(sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as integer), 0)) as sum
       |	from rma_line_items rli
       |	left outer join rma_line_item_skus sli on (sli.id = rli.id)
       |	left outer join skus sku on (skus.id = sli.sku_id)
       |	left outer join object_forms sku_form on (sku_form.id = sku.form_id)
       |	left outer join object_shadows sku_shadow on (sku_shadow.id = sli.sku_shadow_id)
       |	left outer join rma_line_item_gift_cards gcli on (gcli.id = rli.id)
       |	left outer join gift_cards gc on (gc.id = gcli.gift_card_id)
       |	where rli.rma_id = ${rma.id}
       | """.stripMargin.as[(Int, Int)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total.some
      case _                                 ⇒ None
    }
}
