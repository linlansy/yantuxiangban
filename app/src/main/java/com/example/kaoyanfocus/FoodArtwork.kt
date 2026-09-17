package com.example.kaoyanfocus

/** Pre-generated, offline food illustrations. Missing entries fall back to the user's photo. */
fun foodArtworkResource(foodKey: String?): Int? {
    val key = foodKey ?: return null
    val resourceId = runCatching {
        R.drawable::class.java.getField("food_art_$key").getInt(null)
    }.getOrDefault(0)
    return resourceId.takeIf { it != 0 }
}
