/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cahier.developer.brushgraph.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.cahier.developer.brushgraph.data.DisplayText

@Composable
fun DisplayText.asString(): String =
    when (this) {
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
