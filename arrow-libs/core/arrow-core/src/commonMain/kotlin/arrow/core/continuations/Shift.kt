@file:OptIn(ExperimentalTypeInference::class)

package arrow.core.continuations

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.identity
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

@DslMarker
public annotation class EffectDSL

public interface Shift<in R> {
  public fun <B> shift(r: R): B
  
  public fun <B> EagerEffect<R, B>.bind(): B = invoke(this@Shift)
  public operator fun <B> EagerEffect<R, B>.invoke(): B = invoke(this@Shift)
  
  public suspend fun <B> Effect<R, B>.bind(): B = invoke(this@Shift)
  public suspend operator fun <B> Effect<R, B>.invoke(): B = invoke(this@Shift)
  
  public fun <B> Either<R, B>.bind(): B = when (this) {
    is Either.Left -> shift(value)
    is Either.Right -> value
  }
  
  // TODO can be inlined with context receivers, and top-level
  public fun <B> Option<B>.bind(transform: Shift<R>.(None) -> B): B =
    when (this) {
      None -> transform(None)
      is Some -> value
    }
  
  // TODO can be inlined with context receivers, and top-level
  public fun <B> Result<B>.bind(transform: (Throwable) -> R): B =
    fold(::identity) { throwable -> shift(transform(throwable)) }
}

@EffectDSL
public inline fun <R, E, A> Shift<R>.catch(
  @BuilderInference action: Shift<E>.() -> A,
  @BuilderInference resolve: Shift<R>.(E) -> A
): A = fold<E, A, A>({ action(this) }, { throw it }, { resolve(it) }, { it })

@EffectDSL
public inline fun <R, E, A> Shift<R>.catch(
  @BuilderInference action: Shift<E>.() -> A,
  @BuilderInference resolve: Shift<R>.(E) -> A,
  @BuilderInference recover: Shift<R>.(Throwable) -> A,
): A = fold({ action(this) }, { recover(it) }, { resolve(it) }, { it })

@EffectDSL
public inline fun <R, A> Shift<R>.attempt(
  @BuilderInference action: Shift<R>.() -> A,
  @BuilderInference recover: Shift<R>.(Throwable) -> A,
): A = fold({ action(this) }, { recover(it) }, { shift(it) }, { it })

@EffectDSL
@JvmName("attemptOrThrow")
public inline fun <reified T : Throwable, R, A> Shift<R>.attempt(
  @BuilderInference action: Shift<R>.() -> A,
  @BuilderInference recover: Shift<R>.(T) -> A,
): A = attempt(action) { if (it is T) recover(it) else throw it }

@EffectDSL
public inline fun <R> Shift<R>.ensure(condition: Boolean, shift: () -> R): Unit =
  if (condition) Unit else shift(shift())

@OptIn(ExperimentalContracts::class)
@EffectDSL
public inline fun <R, B : Any> Shift<R>.ensureNotNull(value: B?, shift: () -> R): B {
  contract { returns() implies (value != null) }
  return value ?: shift(shift())
}
