import scala.collection.mutable.ArrayBuffer

case class LocalMessage(arrayBuffer: ArrayBuffer[String]) 
case class RemoteMessage(arrayBuffer: ArrayBuffer[String]) 
case class AllocateWork(KValue: Int)
  
