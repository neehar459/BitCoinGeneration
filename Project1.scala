import java.security.MessageDigest
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.util.control._
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef
import akka.routing.RoundRobinRouter

object Project1 {
  def main(args: Array[String]): Unit =
    {
      val numberOfZeroes: Int = Integer.parseInt(args(0))
      val system = ActorSystem("BitCoins")
      val listener = system.actorOf(Props[BitCoinListener], name = "listener")
      val master = system.actorOf(Props(new BitMaster(
        4, 500, listener, numberOfZeroes)),
        name = "master")
      master ! StartBitCoinMaster
    }
}

sealed trait BitCoinMessage
case object StartBitCoinMaster extends BitCoinMessage
case class StartBitCoinWorker(length: Int, ZeroesCount: Int) extends BitCoinMessage
case class ReplyToBitCoinMaster(output: ArrayBuffer[String]) extends BitCoinMessage
case class ComputeSHAHashVal(input: ArrayBuffer[String]) extends BitCoinMessage

class BitMaster(WorkersCount: Int, MessageCount: Int, listener: ActorRef, ZeroesCount: Int) extends Actor {

  var result: ArrayBuffer[String] = new ArrayBuffer[String]()
  var results: Int = _
  val workerRouter = context.actorOf(
    Props[WorkerBitCoin].withRouter(RoundRobinRouter(WorkersCount)), name = "workerRouter")
  def receive = {
    case StartBitCoinMaster =>
      for (i <- 0 until MessageCount) workerRouter ! StartBitCoinWorker(10 + i % 10, ZeroesCount)
    case ReplyToBitCoinMaster(output) =>
      result ++= output
      results += 1
      if (results == MessageCount) {
        listener ! ComputeSHAHashVal(result)
        context.stop(self)
      }
  }
}
class WorkerBitCoin extends Actor {
  def receive = {
    case StartBitCoinWorker(length, k) => sender ! {
      ReplyToBitCoinMaster(getBitCoins(length, k))
    }
  }
  def getBitCoins(length: Int, zeroesCount: Int): ArrayBuffer[String] = {
    var bitCoinArray = new ArrayBuffer[String]
    val stringPattern: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwxyz"
    val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256");
    var numberOfStrings = 0
    var eachString = 0
    for (numberOfStrings <- 1 to 20000) {
      var stringToHash: String = ""
      for (eachString <- 1 to length) {
        stringToHash += stringPattern.charAt(Random.nextInt(63))
      }
      stringToHash = "neehar" + stringToHash
      (messageDigest.update(stringToHash.getBytes()))
      val byteArray: Array[Byte] = messageDigest.digest()
      var stringArray = new ArrayBuffer[String]()
      val loop = new Breaks
      var isFound: Boolean = false
      var hexString: String = ""
      loop.breakable {
        for (i <- 0 until byteArray.length) {
          isFound = false;
          var temp: String = Integer.toHexString((0xFF & byteArray(i)))
          temp = if (temp.length() == 1) "0" + temp else temp
          if (temp.charAt(0) == 0 && i == 1 && isFound) {
            bitCoinArray += stringToHash;
          }
          stringArray.+=(temp)
        }
        hexString = stringArray.toArray.mkString("")
        var KVal: String = ""
        for (i <- 0 until zeroesCount) {
          KVal = KVal + "0"
        }
        if (hexString.startsWith(KVal)) {
          isFound = true;
        }
        if (isFound) {
          bitCoinArray += "BitCoin Input String : " + stringToHash + " , BitCoin : " + hexString
        }
      }
    }
    bitCoinArray
  }
}
class BitCoinListener extends Actor {
  def receive = {

    case ComputeSHAHashVal(input) =>
      {
        if (input.length == 0) {
          println("BitCoins Not Found")
        } else {
          for (i <- 0 until input.length) {
            println(input(i))
          }
        }
      }
      context.system.shutdown()
  }
}
