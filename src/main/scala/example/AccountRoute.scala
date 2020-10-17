package example

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import lerna.akka.entityreplication.{ ClusterReplication, ClusterReplicationSettings }

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AccountRoute(system: ActorSystem) {

  private[this] val region = ClusterReplication(system).start(
    typeName = "BankAccount",
    entityProps = BankAccountActor.props,
    settings = ClusterReplicationSettings(system),
    extractEntityId = BankAccountActor.extractEntityId,
    extractShardId = BankAccountActor.extractShardId,
  )

  implicit val timeout: Timeout                     = Timeout(10.seconds)
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher

  val route: Route = pathPrefix("accounts" / Segment) { accountNo =>
    concat(
      get {
        complete {
          (region ? BankAccountActor.GetBalance(accountNo)).map(_.toString + "\n")
        }
      },
      (post & path("deposit")) {
        parameters("amount") { amount =>
          complete {
            (region ? BankAccountActor.Deposit(accountNo, amount.toInt)).map(_.toString + "\n")
          }
        }
      },
      (post & path("withdraw")) {
        parameters("amount") { amount =>
          complete {
            (region ? BankAccountActor.Withdraw(accountNo, amount.toInt)).map(_.toString + "\n")
          }
        }
      },
    )
  }

}
