package services.users

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import failures.NotFoundFailure404
import failures.UserFailures._
import models.cord.{OrderShippingAddresses, Orders}
import models.user.Users.scope._
import models.user.{User, Users}
import models.user.{UserPasswordResets, UserPasswordReset}
import models.location.Addresses
import models.shipping.Shipments
import payloads.UserPayloads._
import responses.UserResponse._
import services._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object AccountManager {

  def toggleDisabled(userId: Int, disabled: Boolean, actor: User)(implicit ec: EC,
                                                                  db: DB,
                                                                  ac: AC): DbResultT[Root] =
    for {
      user ← * <~ Users.mustFindById404(userId)
      updated ← * <~ Users.update(user,
                                  user.copy(isDisabled = disabled, disabledBy = Some(actor.id)))
      _ ← * <~ LogActivity.userDisabled(disabled, user, actor)
    } yield build(updated)

  // TODO: add blacklistedReason later
  def toggleBlacklisted(userId: Int, blacklisted: Boolean, actor: User)(implicit ec: EC,
                                                                        db: DB,
                                                                        ac: AC): DbResultT[Root] =
    for {
      user ← * <~ Users.mustFindById404(userId)
      updated ← * <~ Users.update(
                   user,
                   user.copy(isBlacklisted = blacklisted, blacklistedBy = Some(actor.id)))
      _ ← * <~ LogActivity.userBlacklisted(blacklisted, user, actor)
    } yield build(updated)

  def resetPasswordSend(
      email: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[ResetPasswordSendAnswer] =
    for {
      user ← * <~ Users
              .activeUserByEmail(Option(email))
              .mustFindOneOr(NotFoundFailure404(User, email))
      resetPwInstance ← * <~ UserPasswordReset
                         .optionFromUser(user)
                         .toXor(UserHasNoEmail(user.id).single)
      findOrCreate ← * <~ UserPasswordResets
                      .findActiveByEmail(email)
                      .one
                      .findOrCreateExtended(UserPasswordResets.create(resetPwInstance))
      (resetPw, foundOrCreated) = findOrCreate
      updatedResetPw ← * <~ (foundOrCreated match {
                            case Found ⇒
                              UserPasswordResets.update(resetPw, resetPw.updateCode())
                            case Created ⇒ DbResultT.good(resetPw)
                          })
      _ ← * <~ LogActivity.userRemindPassword(user, updatedResetPw.code)
    } yield ResetPasswordSendAnswer(status = "ok")

  def resetPassword(
      code: String,
      newPassword: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[ResetPasswordDoneAnswer] = {
    for {
      remind ← * <~ UserPasswordResets
                .findActiveByCode(code)
                .mustFindOr(ResetPasswordCodeInvalid(code))
      user         ← * <~ Users.mustFindById404(remind.userId)
      account      ← * <~ Account.mustFindById404(user.accountId)
      accessMethod ← * <~ AccountAccessMethods.mustFindByIdAndName(account.id, "login")
      _ ← * <~ UserPasswordResets.update(remind,
                                         remind.copy(state = UserPasswordReset.PasswordRestored,
                                                     activatedAt = Instant.now.some))
      updatedAccess ← * <~ AccountAccessMethods.update(accessMethod,
                                                       accessMethod.updatePassword(newPassword))
      _ ← * <~ LogActivity.userPasswordReset(user)
    } yield ResetPasswordDoneAnswer(status = "ok")
  }

  private def resolvePhoneNumber(userId: Int)(implicit ec: EC): DbResultT[Option[String]] = {
    def resolveFromShipments(userId: Int) =
      (for {
        order    ← Orders if order.userId === userId
        shipment ← Shipments if shipment.cordRef === order.referenceNumber &&
          shipment.shippingAddressId.isDefined
        address ← OrderShippingAddresses if address.id === shipment.shippingAddressId &&
          address.phoneNumber.isDefined
      } yield (address.phoneNumber, shipment.updatedAt)).sortBy {
        case (_, updatedAt)   ⇒ updatedAt.desc.nullsLast
      }.map { case (phone, _) ⇒ phone }.one.map(_.flatten).toXor

    for {
      default ← * <~ Addresses
                 .filter(address ⇒ address.userId === userId && address.isDefaultShipping)
                 .map(_.phoneNumber)
                 .one
                 .map(_.flatten)
                 .toXor
      shipment ← * <~ (if (default.isEmpty) resolveFromShipments(userId)
                       else DbResultT.good(default))
    } yield shipment
  }

  def getById(id: Int)(implicit ec: EC, db: DB): DbResultT[Root] = {
    for {
      users ← * <~ Users
               .filter(_.id === id)
               .withRegionsAndRank
               .mustFindOneOr(NotFoundFailure404(User, id))
      (user, shipRegion, billRegion, rank) = users
      maxOrdersDate ← * <~ Orders.filter(_.userId === id).map(_.placedAt).max.result
      phoneOverride ← * <~ (if (user.phoneNumber.isEmpty) resolvePhoneNumber(id)
                            else DbResultT.good(None))
    } yield
      build(user.copy(phoneNumber = user.phoneNumber.orElse(phoneOverride)),
            shipRegion,
            billRegion,
            rank = rank,
            lastOrderDays = maxOrdersDate.map(DAYS.between(_, Instant.now)))
  }

  def create(payload: CreateUserPayload,
             admin: Option[User] = None)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      user ← * <~ User.buildFromPayload(payload).validate
      _ ← * <~ (if (!payload.isGuest.getOrElse(false))
                  Users.createEmailMustBeUnique(user.email)
                else DbResultT.unit)
      updated ← * <~ Users.create(user)
      response = build(updated)
      _ ← * <~ LogActivity.userCreated(response, admin)
    } yield response

  def update(userId: Int,
             payload: UpdateUserPayload,
             admin: Option[User] = None)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      _       ← * <~ payload.validate
      user    ← * <~ Users.mustFindById404(userId)
      _       ← * <~ Users.updateEmailMustBeUnique(payload.email, userId)
      updated ← * <~ Users.update(user, updatedUser(user, payload))
      _       ← * <~ LogActivity.userUpdated(user, updated, admin)
    } yield build(updated)

  def updatedUser(user: User, payload: UpdateUserPayload): User = {
    val updatedUser = (payload.name, payload.email) match {
      case (Some(name), Some(email)) ⇒ user.copy(isGuest = false)
      case _                         ⇒ user
    }

    updatedUser.copy(name = payload.name.fold(user.name)(Some(_)),
                     email = payload.email.orElse(user.email),
                     phoneNumber = payload.phoneNumber.fold(user.phoneNumber)(Some(_)))
  }

  def activate(userId: Int, payload: ActivateUserPayload, admin: User)(implicit ec: EC,
                                                                       db: DB,
                                                                       ac: AC): DbResultT[Root] =
    for {
      _       ← * <~ payload.validate
      user    ← * <~ Users.mustFindById404(userId)
      _       ← * <~ Users.updateEmailMustBeUnique(user.email, user.id)
      updated ← * <~ Users.update(user, user.copy(name = payload.name.some, isGuest = false))
      response = build(updated)
      _ ← * <~ LogActivity.userActivated(response, admin)
    } yield response
}
