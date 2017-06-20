package core.db

import shapeless._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{Query, Rep}

trait ReturningTableQuery[M <: FoxModel[M], T <: FoxTable[M]] { self: FoxTableQuery[M, T] ⇒
  type Ret
  type PackedRet
  val returningQuery: Query[PackedRet, Ret, Seq]
  val returningLens: Lens[M, Ret]
}

trait ReturningId[M <: FoxModel[M], T <: FoxTable[M]] extends ReturningTableQuery[M, T] {
  self: FoxTableQuery[M, T] ⇒
  type Ret       = M#Id
  type PackedRet = Rep[M#Id]
  val returningQuery: Query[Rep[Int], Int, Seq] = map(_.id)
}

trait ReturningIdAndString[M <: FoxModel[M], T <: FoxTable[M]] extends ReturningTableQuery[M, T] {
  self: FoxTableQuery[M, T] ⇒
  type Ret       = (M#Id, String)
  type PackedRet = (Rep[Int], Rep[String])
}
