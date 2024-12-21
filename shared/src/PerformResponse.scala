import io.circe.Codec

final case class PerformResponse(
  id: Int
) derives Codec
