package models

import scala.language.implicitConversions
import scala.util.{Try, Success}

trait HasTryToEither {
 
  /** A helper that handles conversion from Try to Either[Throwable, T] (a rather Elastic4s-specific peculiarity **/ 
  implicit def toEither[T](t: Try[T]): Either[Throwable,T] = t.transform(s => Success(Right(s)), f => Success(Left(f))).get
  
}