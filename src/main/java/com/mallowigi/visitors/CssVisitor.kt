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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.css.CssFunction
import com.intellij.psi.css.CssTerm
import com.intellij.psi.css.browse.CssColorGutterRenderer
import com.intellij.psi.css.impl.CssElementTypes
import com.intellij.psi.css.impl.util.CssColorPsiUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ColorLineMarkerProvider
import com.mallowigi.search.ColorSearchEngine
import java.awt.Color

class CssVisitor : ColorVisitor() {
  // PSI debug names changed in 262; exported token constants are the supported comparison API.
  private val allowedTypes = setOf(
    CssElementTypes.CSS_IDENT,
    CssElementTypes.CSS_NUMBER,
    CssElementTypes.CSS_HASH,
    CssElementTypes.CSS_FUNCTION
  )

  private val extensions: Set<String> = setOf(
    "css",
    "scss",
    "sass",
    "less",
    "styl",
    "pcss",
    "html",
    "vue",
  )

  override fun suitableForFile(file: PsiFile): Boolean =
    extensions.contains(file.virtualFile?.extension)

  override fun accept(element: PsiElement): Color? {
    val type = PsiUtilCore.getElementType(element)
    if (type !in allowedTypes) return null

    val value = element.text
    if (value !is String) return null
    return ColorSearchEngine.getColor(value, this)
  }

  override fun shouldVisit(): Boolean = config.isCssColorEnabled

  override fun shouldShowGutterIcon(element: PsiElement, range: TextRange): Boolean {
    if (!LineMarkerSettings.getSettings().isEnabled(ColorLineMarkerProvider.INSTANCE)) return true

    // The CSS annotator already owns gutter previews for semantic color terms and functions.
    // Keep this plugin's renderer for color-like identifiers outside those CSS value nodes.
    val nativeColorElement = generateSequence(element) { it.parent }
      .firstOrNull { it is CssFunction || it is CssTerm }
      ?: return true
    val isNativeColor = nativeColorElement !is CssTerm || CssColorPsiUtil.isColorTerm(nativeColorElement)
    return !isNativeColor || CssColorGutterRenderer.create(nativeColorElement) == null
  }

  override fun clone(): HighlightVisitor = CssVisitor()
}
