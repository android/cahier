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
                else -> it
            }
        }
        stringResource(resId, *resolvedArgs.toTypedArray())
    }
}
