package tests.rescala.fullmv.incubator

import org.scalatest.{FlatSpec, Matchers}
import rescala.fullmv.incubator._
import tests.rescala.fullmv.TestWithRemoteHost

class TransactionPhaseBroadcastTest extends FlatSpec with Matchers with TestWithRemoteHost{
  "transaction phase transitions " should "work locally" in {
    val t = Transaction("t").assertLocal
    t.phase should be(Framing)
    t.beginTransaction()
    t.phase should be(Executing)
    t.endTransaction()
    t.phase should be(Completed)
  }

  it should "throw exceptions if illegal" in {
    val t = Transaction("t").assertLocal
    a[IllegalStateException] should be thrownBy {
      t.endTransaction()
    }
    t.beginTransaction()
    a[IllegalStateException] should be thrownBy {
      t.assertLocal.beginTransaction()
    }
    t.endTransaction
    a[IllegalStateException] should be thrownBy {
      t.assertLocal.beginTransaction()
    }
    a[IllegalStateException] should be thrownBy {
      t.assertLocal.endTransaction()
    }
  }

  ifRemote(it should "be reflected remotely") { remote =>
    val t = remote.newTransaction("t")
    t.phase should be(Framing)
    remote.beginTransaction(t)
    t.phase should be(Executing)
    remote.endTransaction(t)
    t.phase should be(Completed)
  }

  ifRemote("transaction ingranation" should "only happen once per host") { remote =>
    val a = Transaction("a").assertLocal
    a.sharedOnHosts.size should be(0)
    val b = remote.newTransaction("b")
    a.sharedOnHosts.size should be(0)
    val c = remote.newTransaction("b")
    a.sharedOnHosts.size should be(0)
    b.union(a)
    a.sharedOnHosts.size should be(1)
    c.union(a)
    a.sharedOnHosts.size should be(1)
  }
}
