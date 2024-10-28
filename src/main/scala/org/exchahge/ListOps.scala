package org.exchahge

object ListOps {
  extension [A](list: List[A]) {
    def removeElement(elem: A): List[A] = {
      val (toElem, fromOrder) = list.span(_ != elem)
      toElem ++ fromOrder.drop(1)
    }
  }
}
