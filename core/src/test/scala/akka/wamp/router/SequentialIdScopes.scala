package akka.wamp.router

import akka.wamp.IdScopes.SessionIdScope

trait SequentialIdScopes {
  
  val scopes = Map(
    'global -> new SessionIdScope {},
    'router -> new SessionIdScope {},
    'session -> new SessionIdScope {}
  )
}