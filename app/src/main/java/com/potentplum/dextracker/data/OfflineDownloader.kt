package com.potentplum.dextracker.data

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.potentplum.dextracker.data.remote.PokeApiService
import com.potentplum.dextracker.domain.Generation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

data class DownloadProgress(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val failed: Int = 0,
    /** Bumped each time a download finishes, so the UI can announce it once. */
    val finishedRuns: Int = 0,
)

/**
 * Saves every Pokédex page and sprite (and the art of owned cards) so the whole app works
 * without a connection. Runs in the app's scope so it carries on while the user browses;
 * pages already cached are skipped, so a stopped download resumes where it left off.
 */
class OfflineDownloader(
    private val context: Context,
    private val pokedex: PokedexRepository,
    private val cards: CardRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            _progress.update { it.copy(running = true, done = 0, total = 0, failed = 0) }
            val failed = AtomicInteger(0)
            try {
                pokedex.ensureSpeciesCached()
                val missing = (1..Generation.MAX_DEX) - pokedex.cachedDetailDexes().toSet()
                val artUrls = cards.ownedImageUrls()
                _progress.update { it.copy(total = Generation.MAX_DEX + artUrls.size) }

                val permits = Semaphore(CONCURRENCY)
                coroutineScope {
                    (1..Generation.MAX_DEX).map { dex ->
                        async {
                            permits.withPermit {
                                val ok = runCatching {
                                    if (dex in missing) pokedex.detail(dex)
                                    cacheImage(PokeApiService.spriteUrl(dex))
                                }.isSuccess
                                if (!ok) failed.incrementAndGet()
                                _progress.update { it.copy(done = it.done + 1, failed = failed.get()) }
                            }
                        }
                    }.awaitAll()
                    artUrls.map { url ->
                        async {
                            permits.withPermit {
                                runCatching { cacheImage(url) }
                                _progress.update { it.copy(done = it.done + 1) }
                            }
                        }
                    }.awaitAll()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Couldn't even get the species list: count the whole run as failed.
                failed.compareAndSet(0, 1)
            } finally {
                _progress.update { it.copy(running = false, failed = failed.get(), finishedRuns = it.finishedRuns + 1) }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private suspend fun cacheImage(url: String) {
        val loader = SingletonImageLoader.get(context)
        val result = loader.execute(ImageRequest.Builder(context).data(url).build())
        check(result is SuccessResult) { "Couldn't load $url" }
    }

    private companion object {
        /** Gentle on PokeAPI's free service. */
        const val CONCURRENCY = 4
    }
}
