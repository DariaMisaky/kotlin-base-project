package com.daria.kotlinbase.shared.base

/**
 * Base for one-off domain operations. Subclasses implement [run] and call sites
 * use [executeNow] (suspend) for direct invocation.
 *
 * Error wrapping is the subclass's responsibility — wrap your call in try/catch
 * and return Result.Error with a parsed message.
 */
abstract class BaseUseCase<in P, R> {
    abstract suspend fun run(params: P): Result<R>

    suspend fun executeNow(params: P): Result<R> = run(params)
}
