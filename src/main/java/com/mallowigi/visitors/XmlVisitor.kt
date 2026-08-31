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

import com.intellij.codeInsight.daemon.LineMarkerSettings
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import com.intellij.ui.ColorLineMarkerProvider
import com.mallowigi.search.ColorMatch
import com.mallowigi.search.ColorSearchEngine
import java.awt.Color

class XmlVisitor : ColorVisitor() {
  // Token constants keep XML and HTML parsing independent from platform-specific debug names.
  private val allowedTypes = setOf(XmlTokenType.XML_DATA_CHARACTERS, XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)

  val extensions: Set<String> = setOf(
    "xml",
    "html",
    "xhtml",
    "vue",
    "svelte",
    "svg",
    "jsx",
    "tsx"
  )

  override fun suitableForFile(file: PsiFile): Boolean =
    extensions.contains(file.virtualFile?.extension)

  override fun accept(element: PsiElement): Color? {
    if (element !is XmlToken || element.tokenType !in allowedTypes) return null

    val value = element.text
    if (value !is String) return null
    return ColorSearchEngine.getColor(value, this)
  }

  override fun canAcceptMultiple(): Boolean = true

  override fun acceptMultiple(element: PsiElement): List<ColorMatch>? {
    if (element !is XmlToken || element.tokenType !in allowedTypes) return null

    val value = element.text
    if (value !is String) return null
    return ColorSearchEngine.getAllColors(value, this)
  }

  override fun shouldVisit(): Boolean = config.isMarkupEnabled

  override fun shouldShowGutterIcon(element: PsiElement, range: TextRange): Boolean {
    if (!LineMarkerSettings.getSettings().isEnabled(ColorLineMarkerProvider.INSTANCE)) return true

    val injectedElement = InjectedLanguageManager.getInstance(element.project)
      .findInjectedElementAt(element.containingFile, range.startOffset)
    // CSS injections, such as an HTML style attribute, have their own color gutter annotator.
    return injectedElement?.language?.id != "CSS"
  }

  override fun clone(): HighlightVisitor = XmlVisitor()
}