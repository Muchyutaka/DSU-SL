package vegabobo.dsusideloader.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test
import vegabobo.dsusideloader.preparation.InstallationStep

class DiagnosticMapperTest {

    @Test
    fun mapsKnownInstallationSteps() {
        assertEquals(
            DiagnosticType.SELINUX_DENIAL,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_SELINUX),
        )
        assertEquals(
            DiagnosticType.SELINUX_DENIAL,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_SELINUX_ROOTLESS),
        )
        assertEquals(
            DiagnosticType.PARTITION_CREATION_FAILED,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_CREATE_PARTITION),
        )
        assertEquals(
            DiagnosticType.INSUFFICIENT_STORAGE,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_NO_AVAIL_STORAGE),
        )
        assertEquals(
            DiagnosticType.EXTERNAL_SD_ALLOCATION,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_EXTERNAL_SDCARD_ALLOC),
        )
        assertEquals(
            DiagnosticType.F2FS_KERNEL,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_F2FS_WRONG_PATH),
        )
        assertEquals(
            DiagnosticType.EXTENTS_TOO_FRAGMENTED,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_EXTENTS),
        )
        assertEquals(
            DiagnosticType.ALREADY_RUNNING_DSU,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS),
        )
        assertEquals(
            DiagnosticType.CANCELLED,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_CANCELED),
        )
        assertEquals(
            DiagnosticType.CORRUPTED_IMAGE,
            DiagnosticMapper.fromStep(InstallationStep.ERROR_CORRUPTED_IMAGE),
        )
    }

    @Test
    fun mapsNonErrorStepsToUnknown() {
        assertEquals(DiagnosticType.UNKNOWN, DiagnosticMapper.fromStep(InstallationStep.INSTALLING))
        assertEquals(DiagnosticType.UNKNOWN, DiagnosticMapper.fromStep(InstallationStep.NOT_INSTALLING))
        assertEquals(DiagnosticType.UNKNOWN, DiagnosticMapper.fromStep(null))
    }

    @Test
    fun genericErrorStepFallsBackToRawErrorClassification() {
        assertEquals(
            DiagnosticType.SELINUX_DENIAL,
            DiagnosticMapper.fromStep(
                InstallationStep.ERROR,
                "W Binder:1 type=1400 audit(0.0:51): avc: denied { getattr } for path=\"/dev/block/mmcblk0p42\"",
            ),
        )
        assertEquals(
            DiagnosticType.CORRUPTED_IMAGE,
            DiagnosticMapper.fromStep(
                InstallationStep.ERROR,
                "java.util.zip.ZipException: not in gzip format",
            ),
        )
        assertEquals(
            DiagnosticType.UNKNOWN,
            DiagnosticMapper.fromStep(InstallationStep.ERROR, "something unexpected"),
        )
    }

    @Test
    fun classifiesRawErrorLines() {
        assertEquals(
            DiagnosticType.INSUFFICIENT_STORAGE,
            DiagnosticMapper.fromRawError("E gsid: free space 24% is below the minimum threshold of 40%"),
        )
        assertEquals(
            DiagnosticType.INSUFFICIENT_STORAGE,
            DiagnosticMapper.fromRawError("java.io.IOException: No space left on device"),
        )
        assertEquals(
            DiagnosticType.EXTERNAL_SD_ALLOCATION,
            DiagnosticMapper.fromRawError("E gsid: realpath failed: /mnt/media_rw/AE5C-6D79/dsu: Permission denied"),
        )
        assertEquals(
            DiagnosticType.SELINUX_DENIAL,
            DiagnosticMapper.fromRawError("E gsid: Failed to get stat for block device: Permission denied"),
        )
        assertEquals(
            DiagnosticType.PARTITION_CREATION_FAILED,
            DiagnosticMapper.fromRawError("Failed to createPartition(system)"),
        )
        assertEquals(
            DiagnosticType.EXTENTS_TOO_FRAGMENTED,
            DiagnosticMapper.fromRawError("E gsid: File is too fragmented, needs more than 512 extents."),
        )
        assertEquals(
            DiagnosticType.F2FS_KERNEL,
            DiagnosticMapper.fromRawError("E gsid: read failed: /sys/fs/f2fs/dm-4/features: No such file"),
        )
        assertEquals(
            DiagnosticType.ALREADY_RUNNING_DSU,
            DiagnosticMapper.fromRawError("We are already running in DynamicSystem"),
        )
        assertEquals(
            DiagnosticType.UNSUPPORTED_IMAGE,
            DiagnosticMapper.fromRawError("java.io.IOException: Unsupported filetype"),
        )
    }

    @Test
    fun classifiesCorruptedArchives() {
        assertEquals(
            DiagnosticType.CORRUPTED_IMAGE,
            DiagnosticMapper.fromRawError("org.tukaani.xz.XZFormatException: Input is not in the .xz format"),
        )
        assertEquals(
            DiagnosticType.CORRUPTED_IMAGE,
            DiagnosticMapper.fromRawError("Unexpected end of ZLIB input stream"),
        )
        assertEquals(
            DiagnosticType.CORRUPTED_IMAGE,
            DiagnosticMapper.fromRawError("archive is truncated"),
        )
    }

    @Test
    fun emptyRawErrorIsUnknown() {
        assertEquals(DiagnosticType.UNKNOWN, DiagnosticMapper.fromRawError(null))
        assertEquals(DiagnosticType.UNKNOWN, DiagnosticMapper.fromRawError(""))
        assertEquals(DiagnosticType.UNKNOWN, DiagnosticMapper.fromRawError("   "))
    }

    @Test
    fun mapsThrowables() {
        assertEquals(
            DiagnosticType.CORRUPTED_IMAGE,
            DiagnosticMapper.fromThrowable(java.io.EOFException("unexpected end of stream")),
        )
        assertEquals(
            DiagnosticType.CORRUPTED_IMAGE,
            DiagnosticMapper.fromThrowable(java.io.IOException("read failed")),
        )
        assertEquals(
            DiagnosticType.UNKNOWN,
            DiagnosticMapper.fromThrowable(IllegalStateException("boom")),
        )
        assertEquals(DiagnosticType.UNKNOWN, DiagnosticMapper.fromThrowable(null))
    }
}
