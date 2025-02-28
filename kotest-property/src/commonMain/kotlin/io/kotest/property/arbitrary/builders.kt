package io.kotest.property.arbitrary

import io.kotest.property.Arb
import io.kotest.property.Classifier
import io.kotest.property.RandomSource
import io.kotest.property.Sample
import io.kotest.property.Shrinker
import io.kotest.property.sampleOf
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

/**
 * Creates a new [Arb] that performs no shrinking, has no edge cases and
 * generates values from the given function.
 */
fun <A> arbitrary(fn: suspend ArbitraryBuilderContext.(RandomSource) -> A): Arb<A> =
   arbitraryBuilder { rs -> fn(rs) }

/**
 * Creates a new [Arb] that performs shrinking using the supplied [Shrinker], has no edge cases and
 * generates values from the given function.
 */
fun <A> arbitrary(shrinker: Shrinker<A>, fn: suspend ArbitraryBuilderContext.(RandomSource) -> A): Arb<A> =
   arbitraryBuilder(shrinker) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that classifies the generated values using the supplied [Classifier], has no edge cases and
 * generates values from the given function.
 */
fun <A> arbitrary(classifier: Classifier<A>, fn: suspend ArbitraryBuilderContext.(RandomSource) -> A): Arb<A> =
   arbitraryBuilder(null, classifier) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that performs shrinking using the supplied [Shrinker],
 * classifies the generated values using the supplied [Classifier], has no edge cases and
 * generates values from the given function.
 */
fun <A> arbitrary(
   shrinker: Shrinker<A>,
   classifier: Classifier<A>,
   fn: suspend ArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> =
   arbitraryBuilder(shrinker, classifier) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that performs no shrinking, uses the given edge cases and
 * generates values from the given function.
 */
fun <A> arbitrary(edgecases: List<A>, fn: suspend ArbitraryBuilderContext.(RandomSource) -> A): Arb<A> =
   object : Arb<A>() {
      override fun edgecase(rs: RandomSource): A? = if (edgecases.isEmpty()) null else edgecases.random(rs.random)
      override fun sample(rs: RandomSource): Sample<A> = delegate.sample(rs)

      private val delegate = arbitraryBuilder { rs -> fn(rs) }
   }

/**
 * Creates a new [Arb] that performs shrinking using the supplied [Shrinker], uses the given edge cases and
 * generates values from the given function.
 */
fun <A> arbitrary(
   edgecases: List<A>,
   shrinker: Shrinker<A>,
   fn: suspend ArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = object : Arb<A>() {
   override fun edgecase(rs: RandomSource): A? = if (edgecases.isEmpty()) null else edgecases.random(rs.random)
   override fun sample(rs: RandomSource): Sample<A> = delegate.sample(rs)

   private val delegate = arbitraryBuilder(shrinker) { rs -> fn(rs) }
}

/**
 * Creates a new [Arb] that generates edge cases from the given [edgecaseFn] function
 * and generates samples from the given [sampleFn] function.
 */
fun <A> arbitrary(
   edgecaseFn: (RandomSource) -> A?,
   sampleFn: suspend ArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> =
   object : Arb<A>() {
      override fun edgecase(rs: RandomSource): A? = edgecaseFn(rs)
      override fun sample(rs: RandomSource): Sample<A> = delegate.sample(rs)

      private val delegate: Arb<A> = arbitraryBuilder { rs -> sampleFn(rs) }
   }

/**
 * Creates a new [Arb] that generates edge cases from the given [edgecaseFn] function,
 * performs shrinking using the supplied [Shrinker], and generates samples from the given [sampleFn] function.
 */
fun <A> arbitrary(
   edgecaseFn: (RandomSource) -> A?,
   shrinker: Shrinker<A>,
   sampleFn: suspend ArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> =
   object : Arb<A>() {
      override fun edgecase(rs: RandomSource): A? = edgecaseFn(rs)
      override fun sample(rs: RandomSource): Sample<A> = delegate.sample(rs)

      private val delegate: Arb<A> = arbitraryBuilder(shrinker) { rs -> sampleFn(rs) }
   }

/**
 * Creates a new [Arb] that performs no shrinking, has no edge cases and
 * generates values from the given function.
 */
suspend inline fun <A> generateArbitrary(
   crossinline fn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = suspendArbitraryBuilder { rs -> fn(rs) }

/**
 * Creates a new [Arb] that performs shrinking using the supplied [Shrinker], has no edge cases and
 * generates values from the given function.
 */
suspend inline fun <A> generateArbitrary(
   shrinker: Shrinker<A>,
   crossinline fn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = suspendArbitraryBuilder(shrinker, null) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that classifies the generated values using the supplied [Classifier], has no edge cases and
 * generates values from the given function.
 */
suspend inline fun <A> generateArbitrary(
   classifier: Classifier<A>,
   crossinline fn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = suspendArbitraryBuilder(null, classifier) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that performs shrinking using the supplied [Shrinker],
 * classifies the generated values using the supplied [Classifier], has no edge cases and
 * generates values from the given function.
 */
suspend inline fun <A> generateArbitrary(
   shrinker: Shrinker<A>,
   classifier: Classifier<A>,
   crossinline fn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = suspendArbitraryBuilder(shrinker, classifier) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that performs no shrinking, uses the given edge cases and
 * generates values from the given function.
 */
suspend inline fun <A> generateArbitrary(
   edgecases: List<A>,
   crossinline fn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = suspendArbitraryBuilder(null, null,
   if (edgecases.isEmpty()) null else { rs -> edgecases.random(rs.random) }
) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that performs shrinking using the supplied [Shrinker], uses the given edge cases and
 * generates values from the given function.
 */
suspend inline fun <A> generateArbitrary(
   edgecases: List<A>,
   shrinker: Shrinker<A>,
   crossinline fn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = suspendArbitraryBuilder(
   shrinker,
   null,
   if (edgecases.isEmpty()) null else { rs -> edgecases.random(rs.random) }
) { rs -> fn(rs) }

/**
 * Creates a new [Arb] that generates edge cases from the given [edgecaseFn] function
 * and generates samples from the given [sampleFn] function.
 */
suspend inline fun <A> generateArbitrary(
   crossinline edgecaseFn: (RandomSource) -> A?,
   crossinline sampleFn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> {
   val delegate: Arb<A> = suspendArbitraryBuilder { rs -> sampleFn(rs) }

   return object : Arb<A>() {
      override fun edgecase(rs: RandomSource): A? = edgecaseFn(rs)
      override fun sample(rs: RandomSource): Sample<A> = delegate.sample(rs)
   }
}

/**
 * Creates a new [Arb] that generates edge cases from the given [edgecaseFn] function,
 * performs shrinking using the supplied [Shrinker], and generates samples from the given [sampleFn] function.
 */
suspend inline fun <A> generateArbitrary(
   crossinline edgecaseFn: (RandomSource) -> A?,
   shrinker: Shrinker<A>,
   crossinline sampleFn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> {
   val delegate: Arb<A> = suspendArbitraryBuilder(shrinker) { rs -> sampleFn(rs) }

   return object : Arb<A>() {
      override fun edgecase(rs: RandomSource): A? = edgecaseFn(rs)
      override fun sample(rs: RandomSource): Sample<A> = delegate.sample(rs)
   }
}

/**
 * Creates a new [Arb] using [Continuation] using a stateless [builderFn].
 *
 * This function accepts an optional [shrinker], [classifier], and [edgecaseFn]. These parameters
 * will be passed to [ArbitraryBuilder].
 */
fun <A> arbitraryBuilder(
   shrinker: Shrinker<A>? = null,
   classifier: Classifier<A>? = null,
   edgecaseFn: EdgecaseFn<A>? = null,
   builderFn: suspend ArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = object : Arb<A>() {
   override fun edgecase(rs: RandomSource): A? = singleShotArb().edgecase(rs)
   override fun sample(rs: RandomSource): Sample<A> = singleShotArb().sample(rs)
   override val classifier: Classifier<out A>? = classifier

   /**
    * This function generates a new instance of a single shot arb.
    * DO NOT CACHE THE [Arb] returned by this function.
    *
    * This needs to be a function because at time of writing, Kotlin 1.5's [Continuation] is single shot.
    * With arbs, we ideally need multishot. To rerun [builderFn], we need to "reset" the continuation.
    *
    * The current way we do it is to recreate a fresh [SingleShotArbContinuation] instance that
    * will provide another single shot Arb. Hence the reason why this function is invoked
    * on every call to [sample] / [edgecase].
    */
   private fun singleShotArb(): Arb<A> {
      val restrictedContinuation = SingleShotArbContinuation.Restricted {
         /**
          * At the end of the suspension we got a generated value [A] as a comprehension result.
          * This value can either be a sample, or an edgecase.
          */
         val value: A = builderFn(randomSource.bind())

         /**
          * Here we point A into an Arb<A> with the appropriate enrichments including
          * [Shrinker], [Classifier], and [EdgecaseFn]. When edgecase returns null, we pass the generated value
          * to the edgecase function so to make sure we retain all arbs' edgecases inside the comprehension.
          */
         ArbitraryBuilder({ value }, classifier, shrinker, { rs -> edgecaseFn?.invoke(rs) ?: value }).build()
      }

      return with(restrictedContinuation) {
         this@with.createSingleShotArb()
      }
   }
}

/**
 * Creates a new suspendable [Arb] using [Continuation] using a stateless [fn].
 *
 * This function accepts an optional [shrinker], [classifier], and [edgecaseFn]. These parameters
 * will be passed to [ArbitraryBuilder].
 */
suspend fun <A> suspendArbitraryBuilder(
   shrinker: Shrinker<A>? = null,
   classifier: Classifier<A>? = null,
   edgecaseFn: EdgecaseFn<A>? = null,
   fn: suspend GenerateArbitraryBuilderContext.(RandomSource) -> A
): Arb<A> = suspendCoroutineUninterceptedOrReturn { cont ->
   val arb = object : Arb<A>() {
      override fun edgecase(rs: RandomSource): A? = singleShotArb().edgecase(rs)
      override fun sample(rs: RandomSource): Sample<A> = singleShotArb().sample(rs)
      override val classifier: Classifier<out A>? = classifier

      /**
       * This function generates a new instance of a single shot arb.
       * DO NOT CACHE THE [Arb] returned by this function.
       *
       * This needs to be a function because at time of writing, Kotlin 1.5's [Continuation] is single shot.
       * With arbs, we ideally need multishot. To rerun [fn], we need to "reset" the continuation.
       *
       * The current way we do it is to recreate a fresh [SingleShotArbContinuation] instance that
       * will provide another single shot Arb. Hence the reason why this function is invoked
       * on every call to [sample] / [edgecase].
       */
      private fun singleShotArb(): Arb<A> {
         val suspendableContinuation = SingleShotArbContinuation.Suspendedable(cont.context) {
            /**
             * At the end of the suspension we got a generated value [A] as a comprehension result.
             * This value can either be a sample, or an edgecase.
             */
            val value: A = fn(randomSource.bind())

            /**
             * Here we point A into an Arb<A> with the appropriate enrichments including
             * [Shrinker], [Classifier], and [EdgecaseFn]. When edgecase returns null, we pass the generated value
             * to the edgecase function so to make sure we retain all arbs' edgecases inside the comprehension.
             */
            ArbitraryBuilder({ value }, classifier, shrinker, { rs -> edgecaseFn?.invoke(rs) ?: value }).build()
         }

         return with(suspendableContinuation) {
            this@with.createSingleShotArb()
         }
      }
   }

   cont.resume(arb)
}

/**
 * passthrough arb to extract the propagated RandomSource. It's important to pass rs through both the
 * sample and the edgecases to ensure that flatMap can evaluate on both [sample] and [edgecase]
 * regardless of any absence of edgecases in the firstly bound arb.
 */
private val randomSource: Arb<RandomSource> = ArbitraryBuilder.create { it }.withEdgecaseFn { it }.build()

typealias SampleFn<A> = (RandomSource) -> A
typealias EdgecaseFn<A> = (RandomSource) -> A?

class ArbitraryBuilder<A>(
   private val sampleFn: SampleFn<A>,
   private val classifier: Classifier<A>?,
   private val shrinker: Shrinker<A>?,
   private val edgecaseFn: EdgecaseFn<A>?,
) {
   companion object {
      fun <A> create(f: (RandomSource) -> A): ArbitraryBuilder<A> = ArbitraryBuilder(f, null, null, null)
   }

   fun withClassifier(classifier: Classifier<A>) = ArbitraryBuilder(sampleFn, classifier, shrinker, edgecaseFn)
   fun withShrinker(shrinker: Shrinker<A>) = ArbitraryBuilder(sampleFn, classifier, shrinker, edgecaseFn)
   fun withEdgecaseFn(edgecaseFn: EdgecaseFn<A>) = ArbitraryBuilder(sampleFn, classifier, shrinker, edgecaseFn)
   fun withEdgecases(edgecases: List<A>) = ArbitraryBuilder(sampleFn, classifier, shrinker) {
      if (edgecases.isEmpty()) null else edgecases.random(it.random)
   }

   fun build() = object : Arb<A>() {
      override val classifier: Classifier<out A>? = this@ArbitraryBuilder.classifier
      override fun edgecase(rs: RandomSource): A? = edgecaseFn?.invoke(rs)
      override fun sample(rs: RandomSource): Sample<A> {
         val sample = sampleFn(rs)
         return if (shrinker == null) Sample(sample) else sampleOf(sample, shrinker)
      }
   }
}

interface BaseArbitraryBuilderSyntax {
   /**
    * [bind] returns the generated value of an arb. This can either be a sample or an edgecase.
    */
   suspend fun <T> Arb<T>.bind(): T
}

@RestrictsSuspension
interface ArbitraryBuilderContext : BaseArbitraryBuilderSyntax

interface GenerateArbitraryBuilderContext : BaseArbitraryBuilderSyntax

sealed class SingleShotArbContinuation<F : BaseArbitraryBuilderSyntax, A>(
   override val context: CoroutineContext,
   private val fn: suspend F.() -> Arb<A>
) : Continuation<Arb<A>>, BaseArbitraryBuilderSyntax {
   class Restricted<A>(
      fn: suspend ArbitraryBuilderContext.() -> Arb<A>
   ) : SingleShotArbContinuation<ArbitraryBuilderContext, A>(EmptyCoroutineContext, fn), ArbitraryBuilderContext

   class Suspendedable<A>(
      override val context: CoroutineContext,
      fn: suspend GenerateArbitraryBuilderContext.() -> Arb<A>
   ) : SingleShotArbContinuation<GenerateArbitraryBuilderContext, A>(context, fn), GenerateArbitraryBuilderContext

   private lateinit var returnedArb: Arb<A>
   private var hasExecuted: Boolean = false

   override fun resumeWith(result: Result<Arb<A>>) {
      hasExecuted = true
      result.map { resultArb -> returnedArb = resultArb }.getOrThrow()
   }

   override suspend fun <T> Arb<T>.bind(): T = suspendCoroutineUninterceptedOrReturn { c ->
      // we call flatMap on the bound arb, and then returning the `returnedArb`, without modification
      returnedArb = this.flatMap { value: T ->
         /**
          * we resume the suspension with the value passed inside the flatMap function.
          * this [value] can be either sample or edgecases. This is important
          * because from the point of view of a user of kotest, when we talk about transformation,
          * we care about transforming the generated value of this arb for both sample and edgecases.
          */
         c.resume(value)
         returnedArb
      }
      /**
       * Notice this block returns the special COROUTINE_SUSPENDED value
       * this means the Continuation provided to the block shall be resumed by invoking [resumeWith]
       * at some moment in the future when the result becomes available to resume the computation.
       */
      COROUTINE_SUSPENDED
   }

   /**
    * It's important to understand that at the time of writing (Kotlin 1.5) [Continuation] is single shot,
    * i.e. it can only be resumed once. When it's possible to create multishot continuations in the future, we
    * might be able to simplify this further.
    *
    * The aforementioned limitation means the [Arb] that we construct through this mechanism can only be used
    * to generate exactly one value. Hence, to recycle and rerun the specified composed transformation,
    * we need to recreate the [SingleShotArbContinuation] instance and call [createSingleShotArb] again.
    */
   fun F.createSingleShotArb(): Arb<A> {
      require(!hasExecuted) { "continuation has already been executed, if you see this error please raise a bug report" }
      fn.startCoroutineUninterceptedOrReturn(this@createSingleShotArb, this@SingleShotArbContinuation)
      return returnedArb
   }
}
