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

  implicit val timeout: Timeout = Timeout(10.seconds)

  val route: Route = pathPrefix("accounts" / Segment) { accountNo =>
    {
      val entityRef = replication.entityRefFor(BankAccountBehavior.TypeKey, accountNo)
      concat(
        get {
          onSuccess(entityRef ? BankAccountBehavior.GetBalance) { result =>
            complete(result.toString + "\n")
          }
        },
        (post & path("deposit")) {
          parameters("amount".as[Int], "transactionId".as[Long]) { (amount, transactionId) =>
            onSuccess(entityRef ? (BankAccountBehavior.Deposit(transactionId, amount, _))) { result =>
              complete(result.toString + "\n")
            }
          }
        },
        (post & path("withdraw")) {
          parameters("amount".as[Int], "transactionId".as[Long]) { (amount, transactionId) =>
            onSuccess(entityRef ? (BankAccountBehavior.Withdraw(transactionId, amount, _))) { result =>
              complete(result.toString + "\n")
            }
          }
        },
      )
    }
  }
}
