import akka.actor._
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import scala.collection.mutable.ArrayBuffer
import akka.routing.RoundRobinRouter
import java.security.MessageDigest
import scala.util.control.Breaks
import scala.util.Random

case class StartMaster(kValue: Int)
case class StartWorker(stringlen: Int, kValue: Int)
case class ReplyToMaster(output: ArrayBuffer[String])
case class SHAHashVal(input: ArrayBuffer[String])

object Client {
  def main(args: Array[String]) {
    val hostname = InetAddress.getLocalHost.getHostName
    val config = ConfigFactory.parseString(
      """akka{
		  		actor{
		  			provider = "akka.remote.RemoteActorRefProvider"
		  		}
		  		remote{
                   enabled-transports = ["akka.remote.netty.tcp"]
		  			netty.tcp{
						hostname = "127.0.0.1"
						port = 0
					}
				}     
    	}""")
    implicit val system = ActorSystem("LocalSystem", ConfigFactory.load(config))
    val serverCommActor = system.actorOf(Props(new ServerCommActor(args(0))), name = "serverCommActor")
    val listener = system.actorOf(Props(new Listener(serverCommActor)), name = "listener")
    val master = system.actorOf(Props(new Master(2, 25, listener)), name = "master")
    val internalCommActor = system.actorOf(Props(new InternalCommActor(args(0), master)), name = "internalCommActor")
    internalCommActor ! "REQUESTWORK"
  }
}

class InternalCommActor(ip: String, master: ActorRef) extends Actor {
  val remote = context.actorFor("akka.tcp://BitCoinRemoteSystem@" + ip + ":5150/user/RemoteActor")
  def receive = {
    case "REQUESTWORK" =>
      remote ! "REQUEST_WORK"
    case AllocateWork(kValue) =>
      master ! StartMaster(kValue)
    case SHAHashVal(input) =>
      remote ! RemoteMessage(input)
    case msg: String =>
      println(s"LocalActor received message and the k Value is: '$msg'")
  }
}

class ServerCommActor(ip: String) extends Actor {
  val remote = context.actorFor("akka.tcp://BitCoinRemoteSystem@" + ip + ":5150/user/RemoteActor")
  def receive = {
    case LocalMessage(input) =>
      remote ! RemoteMessage(input)
      context.system.shutdown
    case _ =>
      println("Unknown message in Proxyactor")
  }
}

class Master(WorkersCount: Int, MessageCount: Int, listener: ActorRef) extends Actor {
  var result: ArrayBuffer[String] = new ArrayBuffer[String]()
  var nrOfResults: Int = _
  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(WorkersCount)), name = "workerRouter")
  def receive = {
    case StartMaster(kValue) =>
      for (i <- 0 until MessageCount) workerRouter ! StartWorker(10 + i % 10, kValue)
    case ReplyToMaster(output) =>
      result ++= output
      nrOfResults += 1
      if (nrOfResults == MessageCount) {
        listener ! SHAHashVal(result)
        context.stop(self)
      }
    case _ =>
      println("Unknown Message Received")
  }

}

class Worker extends Actor {
  def receive = {
    case StartWorker(stringlen, kValue) =>
      sender ! ReplyToMaster(mineBitCoins(stringlen, kValue))
  }
  def mineBitCoins(stringlen: Int, kValue: Int): ArrayBuffer[String] = {
    val pattern: String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    var byteArray = new ArrayBuffer[String]()
    var numberOfStrings = 0
    var eachString = 0
    for (l <- 0 until 100000) {
      var input: String = "vishwanath"
      for (i <- 0 until stringlen) {
        var k: Int = Random.nextInt(pattern.length())
        input += pattern.charAt(k)
      }
      val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256");
      val password: String = input
      messageDigest.update(password.getBytes())
      val hashByteArray: Array[Byte] = messageDigest.digest()
      var arrayBuffer = new ArrayBuffer[String]()
      var isFound: Boolean = false;
      var temp: String = null
      val loop = Breaks
      loop.breakable {
        for (i <- 0 to hashByteArray.length - 1) {
          var s: String = Integer.toHexString((hashByteArray(i) & 0xFF))
          s = if (s.length() == 1) "0" + s else s
          if (s.charAt(0) == 0 && i == 1 && isFound) {
            byteArray += input;
          }
          arrayBuffer.+=(s)
        }
        temp = arrayBuffer.toArray.mkString("")
        var kValString: String = ""
        for (l <- 1 to kValue) {
          kValString += "0"
        }
        if (temp.startsWith(kValString)) {
          isFound = true;
        }
      }
      if (isFound) {
        byteArray += "Client Bit Coin String : " + input + " Client Bit Coin : " + temp
      }
    }
    byteArray
  }
}

class Listener(communicateToBridge: ActorRef) extends Actor {
  def receive = {
    case SHAHashVal(input) =>
      {
        communicateToBridge ! LocalMessage(input)
      }
  }
}