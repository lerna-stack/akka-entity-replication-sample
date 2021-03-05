package example

import akka.actor.Props
import akka.event.Logging
import lerna.akka.entityreplication.{ ReplicationActor, ReplicationRegion }
import lerna.akka.entityreplication.raft.protocol.SnapshotOffer

object BankAccountActor {
  def props: Props = Props(new BankAccountActor())

  sealed trait Command {
    def accountNo: String
  }
  final case class Deposit(accountNo: String, amount: Int)  extends Command
  final case class Withdraw(accountNo: String, amount: Int) extends Command
  final case class GetBalance(accountNo: String)            extends Command

  final case object ShortBalance
  final case class AccountBalance(balance: Int)
  final case class BalanceChanged(balance: Int, event: DomainEvent)

  sealed trait DomainEvent
  final case class Deposited(amount: Int) extends DomainEvent
  final case class Withdrew(amount: Int)  extends DomainEvent

  final case class Account(balance: Int) {
    def deposit(amount: Int): Account  = copy(balance = balance + amount)
    def withdraw(amount: Int): Account = copy(balance = balance - amount)
  }

  val extractEntityId: ReplicationRegion.ExtractEntityId = {
    case command: Command => (command.accountNo, command)
  }

  val numberOfShards = 100

  val extractShardId: ReplicationRegion.ExtractShardId = {
    case command: Command => (Math.abs(command.accountNo.hashCode) % numberOfShards).toString
  }

  val ANSI_RESET     = "\u001B[0m"
  val ANSI_YELLOW    = "\u001B[33m"
  val ANSI_BLUE      = "\u001B[34m"
  val LEADER_LABEL   = s"${ANSI_YELLOW}[LEADER]${ANSI_RESET}"
  val FOLLOWER_LABEL = s"${ANSI_BLUE}[FOLLOWER]${ANSI_RESET}"
}

import BankAccountActor._

class BankAccountActor extends ReplicationActor[Account] {

  private[this] var account: Account = Account(balance = 0)

  private[this] val logging = Logging(context.system, this)

  override def receiveCommand: Receive = {
    case Deposit(_, amount) =>
      replicate(Deposited(amount)) { event =>
        updateState(event)
        val result = BalanceChanged(account.balance, event)
        sender() ! result
        logging.info(s"${LEADER_LABEL} Deposit: $result")
      }
    case Withdraw(_, amount) if amount > account.balance =>
      ensureConsistency {
        val result = ShortBalance
        sender() ! result
        logging.info(s"${LEADER_LABEL} Withdraw: $result")
      }
    case Withdraw(_, amount) =>
      replicate(Withdrew(amount)) { event =>
        updateState(event)
        val result = BalanceChanged(account.balance, event)
        sender() ! result
        logging.info(s"${LEADER_LABEL} Withdraw: $result")
      }
    case GetBalance(_) =>
      ensureConsistency {
        val result = AccountBalance(account.balance)
        sender() ! AccountBalance(account.balance)
        logging.info(s"${LEADER_LABEL} GetBalance: $result")
      }
  }

  override def receiveReplica: Receive = {
    case event: DomainEvent =>
      updateState(event)
      logging.info(s"${FOLLOWER_LABEL} receiveReplica: {} account: {}", event, account)
    case SnapshotOffer(snapshot: Account) =>
      account = snapshot
  }

  override def currentState: Account = account

  def updateState(event: DomainEvent): Unit =
    event match {
      case Deposited(amount) =>
        account = account.deposit(amount)
      case Withdrew(amount) =>
        account = account.withdraw(amount)
    }
}
