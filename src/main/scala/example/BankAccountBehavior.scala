package example
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import lerna.akka.entityreplication.typed._

import scala.concurrent.duration._
import scala.collection.immutable.ListMap

object BankAccountBehavior {

  val TypeKey: ReplicatedEntityTypeKey[Command] = ReplicatedEntityTypeKey("BankAccount")

  sealed trait Command
  final case class Deposit(transactionId: Long, amount: BigDecimal, replyTo: ActorRef[DepositSucceeded]) extends Command
  final case class Withdraw(transactionId: Long, amount: BigDecimal, replyTo: ActorRef[WithdrawReply])   extends Command
  final case class GetBalance(replyTo: ActorRef[AccountBalance])                                         extends Command
  final case class ReceiveTimeout()                                                                      extends Command
  final case class Stop()                                                                                extends Command
  // DepositReply
  final case class DepositSucceeded(balance: BigDecimal)
  sealed trait WithdrawReply
  final case class ShortBalance()                         extends WithdrawReply
  final case class WithdrawSucceeded(balance: BigDecimal) extends WithdrawReply
  // GetBalanceReply
  final case class AccountBalance(balance: BigDecimal)

  sealed trait DomainEvent
  final case class Deposited(transactionId: Long, amount: BigDecimal) extends DomainEvent
  final case class Withdrew(transactionId: Long, amount: BigDecimal)  extends DomainEvent
  final case class BalanceShorted(transactionId: Long)                extends DomainEvent

  type Effect = lerna.akka.entityreplication.typed.Effect[DomainEvent, Account]

  final case class Account(balance: BigDecimal, resentTransactions: ListMap[Long, DomainEvent]) {

    def deposit(amount: BigDecimal): Account =
      copy(balance = balance + amount)

    def withdraw(amount: BigDecimal): Account =
      copy(balance = balance - amount)

    private[this] val maxResentTransactionSize = 30

    def recordEvent(transactionId: Long, event: DomainEvent): Account =
      copy(resentTransactions = (resentTransactions + (transactionId -> event)).takeRight(maxResentTransactionSize))

    def applyCommand(command: Command): Effect =
      command match {
        case Deposit(transactionId, amount, replyTo) =>
          if (resentTransactions.contains(transactionId)) {
            Effect.reply(replyTo)(DepositSucceeded(balance))
          } else {
            Effect
              .replicate(Deposited(transactionId, amount))
              .thenReply(replyTo)(state => DepositSucceeded(state.balance))
          }
        case Withdraw(transactionId, amount, replyTo) =>
          resentTransactions.get(transactionId) match {
            // Receive a known transaction: replies message based on stored event in resetTransactions
            case Some(_: Withdrew) =>
              Effect.reply(replyTo)(WithdrawSucceeded(balance))
            case Some(_: BalanceShorted) =>
              Effect.reply(replyTo)(ShortBalance())
            case Some(_: Deposited) =>
              Effect.unhandled.thenNoReply()
            // Receive an unknown transaction
            case None =>
              if (balance < amount) {
                Effect
                  .replicate(BalanceShorted(transactionId))
                  .thenReply(replyTo)(_ => ShortBalance())
              } else {
                Effect
                  .replicate(Withdrew(transactionId, amount))
                  .thenReply(replyTo)(state => WithdrawSucceeded(state.balance))
              }
          }
        case GetBalance(replyTo) =>
          Effect.reply(replyTo)(AccountBalance(balance))
        case ReceiveTimeout() =>
          Effect.passivate().thenNoReply()
        case Stop() =>
          Effect.stopLocally()
      }

    def applyEvent(event: DomainEvent): Account =
      event match {
        case Deposited(transactionId, amount) => deposit(amount).recordEvent(transactionId, event)
        case Withdrew(transactionId, amount)  => withdraw(amount).recordEvent(transactionId, event)
        case BalanceShorted(transactionId)    => recordEvent(transactionId, event)
      }
  }

  def apply(entityContext: ReplicatedEntityContext[Command]): Behavior[Command] = {
    Behaviors.setup { context =>
      // ReceiveTimeout will trigger Effect.passivate()
      context.setReceiveTimeout(1.minute, ReceiveTimeout())
      ReplicatedEntityBehavior[Command, DomainEvent, Account](
        entityContext,
        emptyState = Account(BigDecimal(0), ListMap()),
        commandHandler = (state, cmd) => state.applyCommand(cmd),
        eventHandler = (state, evt) => state.applyEvent(evt),
      ).withStopMessage(Stop())
    }
  }
}
