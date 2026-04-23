package com.example.cahier.ui.brushgraph.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.cahier.ui.brushgraph.model.DisplayText

@Composable
fun DisplayText.asString(): String = when (this) {
    is DisplayText.Literal -> text
    is DisplayText.Resource -> {
        val resolvedArgs = args.map {
            when (it) {
                is DisplayText -> it.asString()
                is List<*> -> {
                    val stringList = it.map { item ->
                        when (item) {
                            is DisplayText -> item.asString()
                            else -> item.toString()
                        }
                    }
                    stringList.joinToString(", ")
                }
                else -> it
            }
        }
        stringResource(resId, *resolvedArgs.toTypedArray())
    }
}
