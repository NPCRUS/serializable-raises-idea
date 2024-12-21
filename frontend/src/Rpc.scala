
import chameleon.ext.circe.{*, given}
import io.circe.Json
import io.circe.parser.*
import json.given
import org.scalajs.dom
import org.scalajs.dom.{HttpMethod, RequestInit}
import sloth.*
import sloth.ext.jsdom.client.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Rpc {
  private object Transport extends RequestTransport[Json, Future] {
    override def apply(request: Request[Json]): Future[Json] =
      dom.fetch(
          s"http://localhost:3500/rpc/${request.method.traitName}/${request.method.methodName}",
          new RequestInit {
            method = HttpMethod.POST
            body = request.payload.noSpaces
          }
      ).toFuture.flatMap { response =>
        response.text().toFuture
      }.map { utf8String =>
        parse(utf8String) match {
          case Left(ex) =>
            println(s"cannot parse: ${ex.message}")
            throw ex
          case Right(value) =>
            value
        }
      }

  }

  private val useMockedTransport: Boolean = false
  
  private val client = new ClientCo[Json, Future](Transport, LogHandler.empty[Future])
  val exampleRpc = client.wire[ExampleRpc]
}