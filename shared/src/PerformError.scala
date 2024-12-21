import io.circe.Codec
enum PerformError derives Codec {
  case ErrorA
  case ErrorB(detail: String)
}
