package example

import akka.actor.ActorSystem
import akka.testkit.TestKit
import lerna.akka.entityreplication.testkit.TestReplicationActorProps
import org.scalatest.Inside

class BankAccountActorSpec extends TestKit(ActorSystem("BankAccountActorSpec")) with ActorSpecBase with Inside {

  "A BankAccountActor" should {

    "increase a balance when it receives Deposit" in {
      val bankAccountActor =
        system.actorOf(TestReplicationActorProps(BankAccountActor.props))

      bankAccountActor ! BankAccountActor.Deposit(
        accountNo = "account-1",
        amount = 1000,
      )
      expectMsgType[BankAccountActor.BalanceChanged].balance should be(1000)

      bankAccountActor ! BankAccountActor.Deposit(
        accountNo = "account-1",
        amount = 2000,
      )
      expectMsgType[BankAccountActor.BalanceChanged].balance should be(3000)
    }

    "decrease a balance when it receives Withdraw" in {
      val bankAccountActor =
        system.actorOf(TestReplicationActorProps(BankAccountActor.props))

      bankAccountActor ! BankAccountActor.Deposit(
        accountNo = "account-1",
        amount = 3000,
      )
      expectMsgType[BankAccountActor.BalanceChanged]

      bankAccountActor ! BankAccountActor.Withdraw(
        accountNo = "account-1",
        amount = 1000,
      )
      expectMsgType[BankAccountActor.BalanceChanged].balance should be(2000)

      bankAccountActor ! BankAccountActor.Withdraw(
        accountNo = "account-1",
        amount = 2000,
      )
      expectMsgType[BankAccountActor.BalanceChanged].balance should be(0)
    }

    "reject the request when it receives Withdraw if the balance is less than the request" in {
      val bankAccountActor =
        system.actorOf(TestReplicationActorProps(BankAccountActor.props))

      bankAccountActor ! BankAccountActor.Deposit(
        accountNo = "account-1",
        amount = 3000,
      )
      expectMsgType[BankAccountActor.BalanceChanged]

      bankAccountActor ! BankAccountActor.Withdraw(
        accountNo = "account-1",
        amount = 5000,
      )
      expectMsg(BankAccountActor.ShortBalance)
    }

    "return a current balance when it receives GetBalance" in {
      val bankAccountActor =
        system.actorOf(TestReplicationActorProps(BankAccountActor.props))

      bankAccountActor ! BankAccountActor.Deposit(
        accountNo = "account-1",
        amount = 3000,
      )
      expectMsgType[BankAccountActor.BalanceChanged]

      bankAccountActor ! BankAccountActor.GetBalance(
        accountNo = "account-1",
      )
      expectMsgType[BankAccountActor.AccountBalance].balance should be(3000)
    }
  }
}
