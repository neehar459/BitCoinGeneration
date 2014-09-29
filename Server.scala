import java.net.InetSocketAddress
import scala.collection.mutable.ArrayBuffer
import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.io.IO
import akka.io.Tcp
import akka.io.Tcp.Close
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Connect
import akka.io.Tcp.Connected
import akka.io.Tcp.ConnectionClosed
import akka.io.Tcp.Register
import akka.io.Tcp.Write
import akka.util.ByteString
import akka.actor.ActorSystem
import akka.actor.AddressFromURIString
import akka.actor.Props
import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import java.security.MessageDigest
import scala.util.Random
import akka.actor.ActorRef
import akka.routing.RoundRobinRouter
import scala.util.control.Breaks


case object StartBitCoinMasterServer 
case class StartBitCoinWorkerServer(length: Int, ZeroesCount: Int) 
case class ReplyToBitCoinMasterServer(output: ArrayBuffer[String]) 
case class ComputeSHAHashValServer(input: ArrayBuffer[String])


object Server {
  def main(args: Array[String]) {

    val hostname = InetAddress.getLocalHost.getHostName
    val config = ConfigFactory.parseString(
      """
 	akka{
 	actor{
 	provider = "akka.remote.RemoteActorRefProvider"
 	}
 	remote{
 	enabled-transports = ["akka.remote.netty.tcp"]
 	netty.tcp{
hostname = "192.168.0.16"
port = 5150
}
}     
    }""")
    val k = Integer.parseInt(args(0))
    val system = ActorSystem("BitCoinRemoteSystem", ConfigFactory.load(config))
    val remoteActor = system.actorOf(Props(new RemoteActor(Integer.parseInt(args(0)))), name = "RemoteActor")
    remoteActor ! "Server is Running"
    val listener = system.actorOf(Props(new BitCoinListener(remoteActor)), name = "listener")
      val master = system.actorOf(Props(new BitMaster(
        4, 500, listener, k)),
        name = "master")
    master ! StartBitCoinMasterServer
  }
}

class RemoteActor(kValue: Int) extends Actor {
  var counter:Int = _
  def receive = {
    case "REQUEST_WORK" => {
      sender ! AllocateWork(kValue)
    }
    case RemoteMessage(arrayBuffer) => {
      if(arrayBuffer.length == 0){
        println("No Bit Coins Found")
      }
      for (i <- 0 until arrayBuffer.length) {
         println(arrayBuffer(i))
      }
      counter+=1
      if(counter == 2){
        context.system.shutdown
      }
    }
    case _ => {
      println("Not a Valid message")
    }
    
  }

}

class BitMaster(WorkersCount: Int, MessageCount: Int, listener: ActorRef, ZeroesCount: Int) extends Actor {

  var result: ArrayBuffer[String] = new ArrayBuffer[String]()
  var nrOfResults: Int = _
  val workerRouter = context.actorOf(
    Props[WorkerBitCoin].withRouter(RoundRobinRouter(WorkersCount)), name = "workerRouter")
  def receive = {
    case StartBitCoinMasterServer =>
      for (i <- 0 until MessageCount) workerRouter ! StartBitCoinWorkerServer(10 + i % 10, ZeroesCount)
    case ReplyToBitCoinMasterServer(output) =>
      result ++= output
      nrOfResults += 1
      if (nrOfResults == MessageCount) {
        listener ! ComputeSHAHashValServer(result)
        context.stop(self)
      }
  }
}
class WorkerBitCoin extends Actor {
  def receive = {
    case StartBitCoinWorkerServer(length, k) => sender ! {
      ReplyToBitCoinMasterServer(getBitCoins(length, k))
    }
  }
  def getBitCoins(length: Int, zeroesCount: Int): ArrayBuffer[String] = {
    var bitCoinArray = new ArrayBuffer[String]
    val pattern: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwxyz"
    val mDigest: MessageDigest = MessageDigest.getInstance("SHA-256");
    var numberOfStrings = 0
    var eachString = 0
    for (numberOfStrings <- 1 to 10000) {
      var randomStr: String = ""
      for (eachString <- 1 to length) {
        randomStr += pattern.charAt(Random.nextInt(63))
      }
      randomStr = "neehar" + randomStr
      (mDigest.update(randomStr.getBytes()))
      val byteArray: Array[Byte] = mDigest.digest()
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
            bitCoinArray += randomStr;
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
          bitCoinArray += "Server : Input String : " + randomStr + " : Server : Bitcoin Value : " + hexString
        }
      }
    }
    bitCoinArray
  }
}
class BitCoinListener(remoteWorker:ActorRef) extends Actor {
  def receive = {
	
    case ComputeSHAHashValServer(input) =>
      {
        if(input.length == 0){
          println("BitCoins not found")
        }
        remoteWorker ! RemoteMessage(input)
      }
  }
}
