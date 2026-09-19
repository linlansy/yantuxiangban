package com.example.kaoyanfocus

/** Pre-generated, offline food illustrations. Missing entries fall back to the user's photo. */
fun foodArtworkResource(foodKey: String?): Int? {
    val key = foodKey ?: return null
    // The Cinnamoroll limited treat is intentionally named without the generic
    // food_art_ prefix, so resolve it explicitly instead of force-unwrapping null in UI.
    if (key == "cotton_candy") return R.drawable.food_cotton_candy
    val resourceId = runCatching {
        R.drawable::class.java.getField("food_art_$key").getInt(null)
    }.getOrDefault(0)
    return resourceId.takeIf { it != 0 }
}
