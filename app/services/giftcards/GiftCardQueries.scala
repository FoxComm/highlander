package services.giftcards

object GiftCardQueries

/*
import models.payment.giftcard.GiftCards
import models.{javaTimeSlickMapper, currencyColumnTypeMapper}
import responses.TheResponse
import responses.GiftCardResponse.{RootSimple, buildForList}
import slick.driver.PostgresDriver.api._
import utils.http.CustomDirectives
import utils.http.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object GiftCardQueries {

  def findAllByQuery(query: GiftCards.QuerySeq = GiftCards)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): DbResultT[TheResponse[Seq[RootSimple]]] = {

    val sortedQuery = query.withMetadata.sortAndPageIfNeeded { case (s, giftCard) ⇒
      s.sortColumn match {
        case "id"               ⇒ if (s.asc) giftCard.id.asc               else giftCard.id.desc
        case "originId"         ⇒ if (s.asc) giftCard.originId.asc         else giftCard.originId.desc
        case "originType"       ⇒ if (s.asc) giftCard.originType.asc       else giftCard.originType.desc
        case "subTypeId"        ⇒ if (s.asc) giftCard.subTypeId.asc        else giftCard.subTypeId.desc
        case "code"             ⇒ if (s.asc) giftCard.code.asc             else giftCard.code.desc
        case "state"            ⇒ if (s.asc) giftCard.state.asc            else giftCard.state.desc
        case "currency"         ⇒ if (s.asc) giftCard.currency.asc         else giftCard.currency.desc
        case "originalBalance"  ⇒ if (s.asc) giftCard.originalBalance.asc  else giftCard.originalBalance.desc
        case "currentBalance"   ⇒ if (s.asc) giftCard.currentBalance.asc   else giftCard.currentBalance.desc
        case "availableBalance" ⇒ if (s.asc) giftCard.availableBalance.asc else giftCard.availableBalance.desc
        case "canceledAmount"   ⇒ if (s.asc) giftCard.canceledAmount.asc   else giftCard.canceledAmount.desc
        case "canceledReason"   ⇒ if (s.asc) giftCard.canceledReason.asc   else giftCard.canceledReason.desc
        case "reloadable"       ⇒ if (s.asc) giftCard.reloadable.asc       else giftCard.reloadable.desc
        case "createdAt"        ⇒ if (s.asc) giftCard.createdAt.asc        else giftCard.createdAt.desc
        case other              ⇒ invalidSortColumn(other)
      }
    }

    sortedQuery.result.map(_.map(buildForList)).toTheResponse
  }

  def findAll(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): DbResultT[TheResponse[Seq[RootSimple]]] =
    findAllByQuery(GiftCards)
}
*/