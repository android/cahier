package com.example.cahier.ui.brushgraph.model

sealed class DisplayText {
  data class Resource(val resId: Int, val args: List<Any> = emptyList()) : DisplayText()
  data class Literal(val text: String) : DisplayText()
}
