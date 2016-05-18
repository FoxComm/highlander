package services

import scala.concurrent.duration.FiniteDuration
import scala.collection.mutable.{ListBuffer, Queue}
import akka.actor.{ActorSystem, Cancellable}
import utils.aliases._
import services.jobs.CustomersRankingJob

package object jobs {

  trait SimpleJob {
    def job(): Unit
    val initialDelay: FiniteDuration
    val interval: FiniteDuration
  }

  private[this] val jobs: ListBuffer[SimpleJob] = ListBuffer.empty[SimpleJob]
  private[this] val runningJobs: Queue[Cancellable] = Queue.empty[Cancellable]

  private[jobs] def registerJob(j: SimpleJob): Unit = {
    jobs += j
  }

  def registerJobs()(implicit db: DB): Unit = {
    registerJob(new CustomersRankingJob)
  }

  def startJobs(system: ActorSystem)(implicit ec: EC): Unit = {
    val scheduler = system.scheduler

    for {
      j ← jobs.toList
    } runningJobs += scheduler.schedule(j.initialDelay, j.interval) { j.job() }
  }

  def stopJobs(system: ActorSystem): Unit = {
    for {
      j ← runningJobs
    } j.cancel()
  }

}