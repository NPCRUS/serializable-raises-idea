import scala.concurrent.Future
trait ExampleRpc {
  def perform: Future[Either[PerformError, PerformResponse]]
  // TODO
  // deff perform Future[PerformResponse] raises PerformError
}
