package app.pulse.feature.media

import java.io.File

object MediaLimits {
    const val MAX_UPLOAD_BYTES = 50L * 1024L * 1024L
    const val MAX_IMAGE_DIMENSION = 1600
    const val JPEG_QUALITY = 80
}

enum class MediaKind { IMAGE, VIDEO, FILE, VOICE, LINK }

data class PendingUpload(val localUri: String, val kind: MediaKind, val bytes: Long, val progress: Int = 0)

fun validateUpload(file: File): Result<Unit> =
    if (file.length() <= MediaLimits.MAX_UPLOAD_BYTES) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalArgumentException("Files must be 50 MB or smaller"))
    }

class UploadQueue {
    private val items = linkedMapOf<String, PendingUpload>()

    fun enqueue(upload: PendingUpload): Result<Unit> {
        if (upload.bytes > MediaLimits.MAX_UPLOAD_BYTES) return Result.failure(IllegalArgumentException("Files must be 50 MB or smaller"))
        items[upload.localUri] = upload
        return Result.success(Unit)
    }

    fun cancel(uri: String) {
        items.remove(uri)
    }

    fun snapshot(): List<PendingUpload> = items.values.toList()
}
