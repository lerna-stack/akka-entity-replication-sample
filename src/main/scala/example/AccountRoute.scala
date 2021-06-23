package example

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import lerna.akka.entityreplication.typed._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AccountRoute(system: ActorSystem[_]) {

  private[this] val replication = ClusterReplication(system)

  replication.init(ReplicatedEntity(BankAccountBehavior.TypeKey)(context => BankAccountBehavior(context)))

  implicit val timeout: Timeout                     = Timeout(10.seconds)
  implicit val dispatcher: ExecutionContextExecutor = system.executionContext

  val route: Route = pathPrefix("accounts" / Segment) { accountNo =>
    {
      val entityRef = replication.entityRefFor(BankAccountBehavior.TypeKey, accountNo)
      concat(
        get {
          complete {
            (entityRef ? BankAccountBehavior.GetBalance).map(_.toString + "\n")
          }
        },
        (post & path("deposit")) {
          parameters("amount".as[Int], "transactionId".as[Long]) { (amount, transactionId) =>
            complete {
              (entityRef ? (BankAccountBehavior.Deposit(transactionId, amount, _))).map(_.toString + "\n")
            }
          }
        },
        (post & path("withdraw")) {
          parameters("amount".as[Int], "transactionId".as[Long]) { (amount, transactionId) =>
            complete {
              (entityRef ? (BankAccountBehavior.Withdraw(transactionId, amount, _))).map(_.toString + "\n")
            }
          }
        },
      )
    }
  }
}
