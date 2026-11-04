package app.hain.models.api

import java.io.File

/** T-237: the installed knowledge packs (offline Wikipedia) for the `wikipedia` tool. */
object KnowledgePacks {
    /** Pack files per language ("de", "en"), the biggest pack first; only what is on the phone. */
    fun installed(models: ModelRepository): Map<String, List<File>> =
        models.catalog
            .filter { it.kind == ModelInfo.KIND_KNOWLEDGE && models.isInstalled(it) }
            .sortedByDescending { it.totalSize }
            .flatMap { pack -> pack.lang.map { lang -> lang to models.mainFile(pack) } }
            .groupBy({ it.first }, { it.second })
}
