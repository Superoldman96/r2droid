package top.wsdx233.r2droid.core.data.source

import top.wsdx233.r2droid.util.R2PipeManager
import java.io.InputStream
import javax.inject.Inject

class R2PipeDataSource @Inject constructor() : R2DataSource {
    override suspend fun execute(cmd: String): Result<String> {
        return R2PipeManager.execute(cmd)
    }

    override suspend fun executeJson(cmd: String): Result<String> {
        return R2PipeManager.executeJson(cmd)
    }

    override suspend fun <T> executeStream(cmd: String, block: suspend (InputStream) -> T): Result<T> {
        return R2PipeManager.executeStream(cmd, block)
    }
}
