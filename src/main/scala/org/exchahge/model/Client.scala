package org.exchahge.model

case class Client(name: String) extends AnyVal {
  override def toString: String = name
}
