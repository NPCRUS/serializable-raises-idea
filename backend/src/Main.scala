import io.circe.{Decoder, Encoder, Json, ParsingFailure}
import zio.http.{HandlerAspect, *}
import io.circe.parser.*
import chameleon.ext.circe.{*, given}
import sloth.Router
import zio.http.Middleware.{CorsConfig, cors, requestLogging}
import zio.{ZIO, ZIOAppDefault}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import zio.http.Server.RequestStreaming
import io.circe.Codec
import json.given

object ZioHttpServer extends ZIOAppDefault {
  val corsConfig = CorsConfig(
    allowedOrigin = _ => Some(Header.AccessControlAllowOrigin.All)
  )

  val impl = new ExampleRpc {
    override def perform: Future[Either[PerformError, PerformResponse]] = 
      Future.successful(Left(PerformError.ErrorB("you are fired")))
  }
  val rpcRouter = Router[Json, Future]
    .route[ExampleRpc](impl)

  val routes =
    Routes(
      Method.GET / Root / "health" -> Handler.text("alive"),
      Method.POST / Root / "rpc" / string("api") / string ("method") ->
        handler { (api: String, method: String, request: Request) =>
          for {
            entityStr <- request.body.asString(Charsets.Utf8)
            json <- ZIO.fromEither(parse(entityStr))
            result <- ZIO.fromFuture { context =>
              rpcRouter(sloth.Request(sloth.Method(api, method), json)) match
                case Left(error) =>
                  Future.successful(Left(error))
                case Right(value) =>
                  value.map(Right.apply)
            }.absolve
          } yield Response.json(result.spaces2)
        }.catchAll {
          case e: ParsingFailure =>
            println(e)
            Handler.badRequest(s"cannot parse json: ${e.message}")
          case e: sloth.ServerFailure.DeserializerError =>
            println(e.toException)
            Handler.badRequest(e.ex.getMessage)
          case e: sloth.ServerFailure.MethodNotFound =>
            println(e.toException)
            handler(Response.notFound(s"method not found: ${e.path}"))
          case e: sloth.ServerFailure.HandlerError =>
            println(e.ex)
            handler(Response.internalServerError(e.ex.getMessage))
          case e: Exception =>
            e.printStackTrace()
            println(e)
            Handler.internalServerError(e.getMessage)
          case e: Throwable =>
            println(e)
            handler(Response.internalServerError(e.getMessage))
        }
    )

  def run = Server.serve(
    routes @@ cors(corsConfig) @@ requestLogging(
      logRequestBody = true,
      logResponseBody = true
    )
  ).provide(
    Server.defaultWith(
      _.port(3500)
        .requestStreaming(RequestStreaming.Enabled)
        .logWarningOnFatalError(true)
    )
  )
}
