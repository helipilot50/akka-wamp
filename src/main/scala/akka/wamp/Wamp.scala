package akka.wamp


import akka.actor.{ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.io.IO


/**
  * WAMP Extension for Akka’s IO layer.
  *
  * For a full description of the design and philosophy behind this IO
  * implementation please refer to <a href="http://doc.akka.io/">the Akka online documentation</a>.
  *
  * In order to open an outbound connection send a [[Wamp.Connect]] message
  * to the [[WampExtension#manager]].
  *
  * In order to start listening for inbound connections send a [[Wamp.Bind]]
  * message to the [[WampExtension#manager]].
  */
object Wamp extends ExtensionId[WampExtension] with ExtensionIdProvider {
  
  /**
    * Returns the canonical ExtensionId for this Extension
    */
  override def lookup(): ExtensionId[_ <: Extension] = Wamp

  /**
    * Is used by Akka to instantiate the Extension identified by this ExtensionId,
    * internal use only.
    */
  override def createExtension(system: ExtendedActorSystem): WampExtension = new WampExtension(system)

  /**
    * The common interface for [[Command]]s and [[Signal]]s.
    */
  trait AbstractMessage

  /**
    * This is the common trait for all commands understood by TCP actors.
    */
  trait Command extends AbstractMessage

  /**
    * The Connect message is sent to the WAMP manager actor, which is obtained via
    * [[WampExtension#manager]]. Either the manager replies with a [[CommandFailed]]
    * or the actor handling the new connection replies with a [[Connected]]
    * message.
    *
    * @param client is the client actor reference
    * @param url is the URI to connect to (e.g. "ws://somehost.com:9999/path/to/ws")
    * @param subprotocol is the WebSocket subprotocol to negotiate (e.g. "wamp.2.msgpack" or  "wamp.2.json")
    */
  final case class Connect(client: ActorRef, url: String = "ws://localhost:8080/ws", subprotocol: String = "wamp.2.json") extends Command

  final case object Disconnect extends Command
  
  /**
    * The Bind message is send to the WAMP manager actor, which is obtained via
    * [[WampExtension#manager]] in order to bind to a listening socket. The manager
    * replies either with a [[CommandFailed]] or the actor handling the listen
    * socket replies with a [[Bound]] message. If the local port is set to 0 in
    * the Bind message, then the [[Bound]] message should be inspected to find
    * the actual port which was bound to.
    *
    * @param router is the actor which will receive all incoming connection requests in the form of [[Connected]] messages
    */
  final case class Bind(router: ActorRef) extends Command


  /**
    * In order to close down a listening socket, send this message to that socket’s
    * actor (that is the actor which previously had sent the [[Bound]] message). The
    * listener socket actor will reply with a [[Unbound]] message.
    */
  final case object Unbind


  /**
    * Common interface for all signals generated by the Wammp layer actors.
    */
  trait Signal extends AbstractMessage

  
  /**
    * The sender of a [[Bind]] command will — in case of success — receive confirmation
    * in this form. If the bind address indicated a 0 port number, then the contained
    * `localAddress` can be used to find out which port was automatically assigned.
    */
  final case class Bound(url: String) extends Signal

  final case class BindFailed(cause: Throwable) extends Signal
  
  /**
    * The connection actor sends this message either to the sender of a [[Connect]]
    * command (for outbound) or to the handler for incoming connections designated
    * in the [[Bind]] message. 
    */
  final case class Connected(peer: ActorRef) extends Signal

  final case class ConnectionFailed(ex: Throwable) extends Signal

  /**
    * The disconnected signal to announce transport disconnection
    */
  final case object Disconnected extends Signal
  
}



class WampExtension(system: ExtendedActorSystem) extends IO.Extension {
  val manager = system.actorOf(Manager.props(), name = "IO-Wamp")
}
