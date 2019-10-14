package com.sksamuel.hoplite.decoder

import arrow.core.invalid
import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.ArrayNode
import com.sksamuel.hoplite.DecoderContext
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.arrow.flatMap
import com.sksamuel.hoplite.arrow.sequence
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

class SetDecoder : NonNullableDecoder<Set<*>> {

  override fun supports(type: KType): Boolean = type.isSubtypeOf(Set::class.starProjectedType)

  override fun safeDecode(node: Node,
                          type: KType,
                          context: DecoderContext): ConfigResult<Set<*>> {
    require(type.arguments.size == 1)

    val t = type.arguments[0].type!!

    fun <T> decode(node: ArrayNode, decoder: Decoder<T>): ConfigResult<Set<T>> {
      return node.elements.map { decoder.decode(it, type, context) }.sequence()
        .leftMap { ConfigFailure.CollectionElementErrors(node, it) }
        .map { it.toSet() }
    }

    fun <T> decode(node: StringNode, decoder: Decoder<T>): ConfigResult<Set<T>> {
      val tokens = node.value.split(",").map {
        StringNode(it.trim(), node.pos)
      }
      return tokens.map { decoder.decode(it, type, context) }.sequence()
        .leftMap { ConfigFailure.CollectionElementErrors(node, it) }
        .map { it.toSet() }
    }

    return context.decoder(t).flatMap { decoder ->
      when (node) {
        is ArrayNode -> decode(node, decoder)
        is StringNode -> decode(node, decoder)
        else -> ConfigFailure.UnsupportedCollectionType(node, "Set").invalid()
      }
    }
  }
}
