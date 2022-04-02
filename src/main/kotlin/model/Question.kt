package model


class Question(val question: String) {

    val variants: MutableList<Variant> = mutableListOf()
    var picture: String? = null

    fun addVariant(value: String, isRight: Boolean) {
        variants.add(if (isRight) Variant.Right(value) else Variant.Wrong(value))
    }
}

sealed class Variant() {
    data class Wrong(val value: String) : Variant()
    data class Right(val value: String) : Variant()
}