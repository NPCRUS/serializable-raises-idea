import io.circe.{Codec, Decoder, Encoder}

package object json {

  // codec for either type
  given [A: Encoder : Decoder, B: Encoder : Decoder]: Codec.AsObject[Either[A, B]] =
    Codec.codecForEither("left", "right")
}
