package example

import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

trait ActorSpecBase extends ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll { this: TestKit =>

  override def afterAll(): Unit = {
    try {
      TestKit.shutdownActorSystem(system)
    } finally super.afterAll()
  }
}
