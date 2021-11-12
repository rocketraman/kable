package com.juul.kable

public sealed class Observation {

    public sealed class Failure {
        public data class Start(val cause: Throwable) : Failure()
    }

    public data class Data(val bytes: ByteArray) : Observation() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Data
            if (!bytes.contentEquals(other.bytes)) return false
            return true
        }

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    public data class Error(val cause: Failure) : Observation()
}
