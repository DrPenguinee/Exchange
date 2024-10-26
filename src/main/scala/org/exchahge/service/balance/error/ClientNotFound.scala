package org.exchahge.service.balance.error

import org.exchahge.model.Client

case class ClientNotFound(client: Client) extends NoSuchElementException(s"Client ${client.name} not found")
