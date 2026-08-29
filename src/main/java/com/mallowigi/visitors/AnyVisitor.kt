/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 Elior "Mallowigi" Boukhobza
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 */

package com.mallowigi.visitors

import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.mallowigi.search.ColorMatch
import com.mallowigi.search.ColorSearchEngine

class AnyVisitor : ColorVisitor() {
  // These IDs mirror plugin.xml's optional descriptors. A loaded language visitor always wins;
  // otherwise this visitor treats the file as text without requiring that language plugin.
  private val languagePlugins: Map<String, String> = mapOf(
    "asp" to "com.intellij.modules.rider",
    "c" to "com.intellij.modules.c-capable",
    "cjs" to "JavaScript",
    "cpp" to "com.intellij.modules.cpp-plugin-capable",
    "cs" to "com.intellij.modules.rider",
    "css" to "com.intellij.css",
    "dart" to "Dart",
    "go" to "com.intellij.modules.go-capable",
    "groovy" to "org.intellij.groovy",
    "h" to "com.intellij.modules.c-capable",
    "html" to "com.intellij.modules.lang",
    "java" to "com.intellij.java",
    "js" to "JavaScript",
    "json" to "com.intellij.modules.json",
    "jsx" to "JavaScript",
    "kt" to "org.jetbrains.kotlin",
    "kts" to "org.jetbrains.kotlin",
    "less" to "com.intellij.css",
    "lua" to "com.tang",
    "md" to "org.intellij.plugins.markdown",
    "mdx" to "org.intellij.plugins.markdown",
    "mjs" to "JavaScript",
    "objc" to "com.intellij.modules.cidr.lang",
    "php" to "com.jetbrains.php",
    "phpt" to "com.jetbrains.php",
    "properties" to "com.intellij.properties",
    "py" to "com.intellij.modules.python",
    "r" to "R4Intellij",
    "rb" to "org.jetbrains.plugins.ruby",
    "rbs" to "org.jetbrains.plugins.ruby",
    "rs" to "com.jetbrains.rust",
    "sass" to "com.intellij.css",
    "scala" to "org.intellij.scala",
    "scss" to "com.intellij.css",
    "sql" to "com.intellij.database",
    "styl" to "com.intellij.css",
    "svelte" to "dev.blachut.svelte.lang",
    "svg" to "com.intellij.modules.lang",
    "swift" to "com.intellij.modules.cidr.lang",
    "toml" to "org.toml.lang",
    "ts" to "JavaScript",
    "tsx" to "JavaScript",
    "vue" to "org.jetbrains.plugins.vue",
    "xml" to "com.intellij.modules.lang",
    "yaml" to "org.jetbrains.plugins.yaml",
    "yml" to "org.jetbrains.plugins.yaml",
  )

  private val configurableTextExtensions: Set<String> = setOf("ini", "log", "rst", "txt")

  override fun suitableForFile(file: PsiFile): Boolean {
    if (!config.isFallbackEnabled) return false

    val extension = file.virtualFile?.extension?.lowercase()
    if (extension in configurableTextExtensions) return false

    val pluginId = languagePlugins[extension] ?: return true
    return !PluginManagerCore.isLoaded(PluginId.getId(pluginId))
  }

  override fun canAcceptMultiple(): Boolean = true

  // Leaf PSI is the only stable representation shared by parsed files, plain text, and unknown files.
  // Scanning leaves also avoids duplicate matches from walking both parent and child PSI ranges.
  override fun acceptMultiple(element: PsiElement): List<ColorMatch>? =
    if (element.firstChild == null && element.text.isNotBlank()) {
      ColorSearchEngine.getAllColors(element.text, this)
    } else {
      null
    }

  override fun clone(): HighlightVisitor = AnyVisitor()
}
