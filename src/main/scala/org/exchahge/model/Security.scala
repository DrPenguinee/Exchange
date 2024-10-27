package org.exchahge.model

enum Security(name: String) {
  case A extends Security("A")
  case B extends Security("B")
  case C extends Security("C")
  case D extends Security("D")
}

object Security {
  def fromValue(s: String): Option[Security] = s match {
    case "A" => Some(Security.A)
    case "B" => Some(Security.B)
    case "C" => Some(Security.C)
    case "D" => Some(Security.D)
    case _   => None
  }
}
