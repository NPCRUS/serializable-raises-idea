import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExportTopLevel
import io.circe.syntax.*

import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  val rootApp = {

    val buttonClickBus = EventBus[Unit]()
    // TODO: here i would like to mend what was being raised
    // using some implicit tooling with laminar
    // in fact i want to have contingency for js much before whole serialization
    // because i want to see how it plays out with whole reactive streaming approach
    val resultStream = buttonClickBus.stream.flatMapSwitch { _ =>
      EventStream.fromFuture(Rpc.exampleRpc.perform)
    }

    div(
      display.flex,
      flexDirection.column, 
      input(
        "Press me",
        `type` := "button",
        onClick.mapToUnit --> buttonClickBus.writer
      ),
      h1(
        color <-- resultStream.map { result =>
          if(result.isRight) "green" else "red"
        },
        "Status: ",
        text <-- resultStream.map {
          case Left(_) => "Error"
          case Right(value) => "Success"
        }
      ),
      pre(
        text <-- resultStream.map {
          case Left(error) => error.asJson.spaces2
          case Right(value) => value.asJson.spaces2
        }
      )
    )
  }
  
  @main
  def main(): Unit = {
    renderOnDomContentLoaded(dom.document.body, rootApp)
  }
}
