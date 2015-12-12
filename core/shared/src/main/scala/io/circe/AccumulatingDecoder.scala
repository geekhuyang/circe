package io.circe

import cats.Applicative
import cats.data.{ Validated, ValidatedNel }
import cats.std.list._

sealed trait AccumulatingDecoder[A] extends Serializable { self =>
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): AccumulatingDecoder.Result[A]

  /**
   * Map a function over this [[AccumulatingDecoder]].
   */
  def map[B](f: A => B): AccumulatingDecoder[B] = new AccumulatingDecoder[B] {
    def apply(c: HCursor): AccumulatingDecoder.Result[B] = self(c).map(f)
  }

  def ap[B](f: AccumulatingDecoder[A => B]): AccumulatingDecoder[B] = new AccumulatingDecoder[B] {
    def apply(c: HCursor): AccumulatingDecoder.Result[B] = self(c).ap(f(c))
  }
}

object AccumulatingDecoder {
  type Result[A] = ValidatedNel[DecodingFailure, A]

  def fromDecoder[A](implicit decode: Decoder[A]): AccumulatingDecoder[A] =
    new AccumulatingDecoder[A] {
      def apply(c: HCursor): Result[A] = decode.decodeAccumulating(c)
    }

  implicit val accumulatingDecoderApplicative: Applicative[AccumulatingDecoder] =
    new Applicative[AccumulatingDecoder] {
      def pure[A](a: A): AccumulatingDecoder[A] = new AccumulatingDecoder[A] {
        def apply(c: HCursor): AccumulatingDecoder.Result[A] = Validated.valid(a)
      }
      override def map[A, B](fa: AccumulatingDecoder[A])(f: A => B): AccumulatingDecoder[B] =
        fa.map(f)
      def ap[A, B](fa: AccumulatingDecoder[A])(
        f: AccumulatingDecoder[A => B]
      ): AccumulatingDecoder[B] = fa.ap(f)
    }
}