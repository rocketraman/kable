@file:Suppress("RedundantUnitReturnType")

package com.juul.kable

import com.juul.kable.Peripheral.Configuration
import com.juul.kable.WriteType.WithoutResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

public expect fun CoroutineScope.peripheral(
    advertisement: Advertisement,
): Peripheral

public expect fun CoroutineScope.peripheral(
    advertisement: Advertisement,
    configuration: Configuration = Configuration.Default,
    builderAction: Configuration.Builder.() -> Unit,
): Peripheral

public enum class WriteType {
    WithResponse,
    WithoutResponse,
}

@OptIn(ExperimentalStdlibApi::class) // for CancellationException in @Throws
public interface Peripheral {

    /**
     * Provides a conflated [Flow] of the [Peripheral]'s [State].
     *
     * After [connect] is called, the [state] will typically transition through the following [states][State]:
     *
     * ```
     *     connect()
     *         :
     *         v
     *   .------------.       .-----------.
     *   | Connecting | ----> | Connected |
     *   '------------'       '-----------'
     *                              :
     *                       disconnect() or
     *                       connection drop
     *                              :
     *                              v
     *                      .---------------.       .--------------.
     *                      | Disconnecting | ----> | Disconnected |
     *                      '---------------'       '--------------'
     * ```
     */
    public val state: Flow<State>

    /**
     * Initiates a connection, suspending until connected, or failure occurs. Multiple concurrent invocations will all
     * suspend until connected (or failure occurs). If already connected, then returns immediately.
     *
     * @throws ConnectionRejectedException when a connection request is rejected by the system (e.g. bluetooth hardware unavailable).
     * @throws IllegalStateException if [Peripheral]'s Coroutine scope has been cancelled.
     */
    public suspend fun connect(): Unit

    /**
     * Disconnects the active connection, or cancels an in-flight [connection][connect] attempt, suspending until
     * [Peripheral] has settled on a [disconnected][State.Disconnected] state.
     *
     * Multiple concurrent invocations will all suspend until disconnected (or failure occurs).
     */
    public suspend fun disconnect(): Unit

    /** @return discovered [services][Service], or `null` until a [connection][connect] has been established. */
    public val services: List<DiscoveredService>?

    /**
     * @throws NotReadyException if invoked without an established [connection][connect].
     */
    @Throws(CancellationException::class, IOException::class, NotReadyException::class)
    public suspend fun rssi(): Int

    /**
     * @throws NotReadyException if invoked without an established [connection][connect].
     */
    @Throws(CancellationException::class, IOException::class, NotReadyException::class)
    public suspend fun read(
        characteristic: Characteristic,
    ): ByteArray

    /**
     * @throws NotReadyException if invoked without an established [connection][connect].
     */
    @Throws(CancellationException::class, IOException::class, NotReadyException::class)
    public suspend fun write(
        characteristic: Characteristic,
        data: ByteArray,
        writeType: WriteType = WithoutResponse,
    ): Unit

    /**
     * @throws NotReadyException if invoked without an established [connection][connect].
     */
    @Throws(CancellationException::class, IOException::class, NotReadyException::class)
    public suspend fun read(
        descriptor: Descriptor,
    ): ByteArray

    /**
     * @throws NotReadyException if invoked without an established [connection][connect].
     */
    @Throws(CancellationException::class, IOException::class, NotReadyException::class)
    public suspend fun write(
        descriptor: Descriptor,
        data: ByteArray,
    ): Unit

    /**
     * Observes changes to the specified [Characteristic].
     *
     * Observations can be setup ([observe] can be called) prior to a [connection][connect] being established. Once
     * connected, the observation will automatically start emitting changes. If connection is lost, [Flow] will remain
     * active, once reconnected characteristic changes will begin emitting again.
     *
     * Failures related to notifications are propagated via [connect] if the [observe] [Flow] is collected prior to a
     * connection being established. If a connection is already established when an [observe] [Flow] collection begins,
     * then notification failures are propagated via the returned [observe] [Flow].
     *
     * If the specified [characteristic] is invalid or cannot be found then a [NoSuchElementException] is propagated.
     */
    public fun observe(
        characteristic: Characteristic,
    ): Flow<ByteArray>

    public companion object {
        @Suppress("FunctionName")
        public fun Configuration(
            builder: Configuration.Builder.() -> Unit
        ): Configuration = Configuration.Builder(Configuration.Default).apply(builder).build()
    }

    public data class Configuration internal constructor(
        public val writeObserveDescriptors: Boolean = true,
    ) {

        public companion object {
            public val Default: Configuration = Configuration()
        }

        public class Builder internal constructor(
            configuration: Configuration
        ) {

            /**
             * By default, when an observation is spun up/down, `BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE`
             * (`0x01`, `0x00`) is written to client config characteristic (0x2902). If a remote peripheral does not
             * properly handle this behavior then it may be disabled by setting [writeObserveDescriptors] to `false`.
             *
             * Android only.
             * Default: `true`
             */
            public var writeObserveDescriptors: Boolean = configuration.writeObserveDescriptors

            internal fun build(): Configuration = Configuration(
                writeObserveDescriptors = writeObserveDescriptors,
            )
        }
    }
}
