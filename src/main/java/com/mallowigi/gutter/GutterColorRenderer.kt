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
package com.mallowigi.gutter

import com.intellij.codeInsight.actions.ReaderModeSettings
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ColorChooserService
import com.intellij.ui.ColorUtil
import com.intellij.ui.picker.ColorListener
import com.intellij.ui.picker.ColorPickerPopupCloseListener
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.EmptyIcon
import com.mallowigi.ColorHighlighterBundle.message
import com.mallowigi.gutter.actions.*
import org.jetbrains.annotations.NonNls
import java.awt.Color
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.swing.Icon

class GutterColorRenderer(private val color: Color?, private val range: TextRange) : GutterIconRenderer() {
  override fun getIcon(): Icon = when {
    color != null -> {
      EditorColorsManager.getInstance().globalScheme.defaultForeground
        .let { ColorIcon(ICON_SIZE, ICON_SIZE, ICON_SIZE - 2, ICON_SIZE - 2, color, it, 3) }
        .let { JBUIScale.scaleIcon(it) }
    }

    else -> JBUIScale.scaleIcon(EmptyIcon.create(ICON_SIZE))
  }

  override fun getTooltipText(): String = message("choose.color")

  override fun getPopupMenuActions(): ActionGroup {
    return DefaultActionGroup(
      CopyAndroidArgb(color),
      CopyAndroidRgb(color),
      CopyHexAction(color),
      CopyRgbAction(color),
      CopyRgbaAction(color),
      CopyHslAction(color),
      CopyHslaAction(color),
      CopyJavaColorResource(color),
      CopyKotlinColorResource(color),
      CopyJavaRgb(color),
      CopyJavaRgba(color),
      CopyKotlinRgb(color),
      CopyKotlinRgba(color),
      CopyNetRgb(color),
      CopyNetArgb(color),
      CopyNSColorHsb(color),
      CopyNSColorHsba(color),
      CopyUIColorHsb(color),
      CopyUIColorHsba(color),
      CopySwiftHsb(color),
      CopySwiftHsba(color)
    )
  }

  override fun getClickAction(): @NonNls AnAction {
    return object : AnAction(message("choose.color1")) {
      override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val currentColor = color ?: return
        val rangeMarker = if (canReplaceColor(editor)) {
          editor.document.createRangeMarker(range).also {
            it.isGreedyToLeft = true
            it.isGreedyToRight = true
          }
        } else {
          null
        }

        // The dialog probes the Robot-based pipette before opening, which can fail on Wayland.
        ColorChooserService.getInstance().showPopup(
          editor.project,
          currentColor,
          editor,
          ColorListener { newColor, _ -> applyColor(editor, rangeMarker, currentColor, newColor) },
          currentColor.alpha != OPAQUE,
          false,
          ColorPickerPopupCloseListener { rangeMarker?.dispose() }
        )
      }

      private fun applyColor(editor: Editor, rangeMarker: RangeMarker?, currentColor: Color, newColor: Color?) {
        val guarded = rangeMarker?.takeIf { it.isValid }?.let {
          editor.document.getRangeGuard(it.startOffset, it.endOffset) != null
        } ?: true

        if (rangeMarker == null || !canReplaceColor(editor) || guarded) {
          copyColor(currentColor, newColor)
        } else {
          replaceColor(editor, rangeMarker, newColor)
        }
      }

      private fun canReplaceColor(editor: Editor): Boolean {
        if (editor.isViewer || !editor.document.isWritable) return false

        val project = editor.project ?: return false
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return false
        if (!virtualFile.isWritable) return false

        // Reader Mode is visual and does not necessarily make the editor or document read-only.
        val readerModeSettings = ReaderModeSettings.Companion.getInstance(project)
        return !readerModeSettings.enabled || !ReaderModeSettings.Companion.matchMode(project, virtualFile, editor)
      }

      private fun copyColor(currentColor: Color, newColor: Color?) {
        if (newColor == null || newColor == currentColor) return

        CopyPasteManager.getInstance().setContents(StringSelection(ColorUtil.toHex(newColor, false)))
      }

      private fun replaceColor(editor: Editor, rangeMarker: RangeMarker, newColor: Color?) {
        if (newColor == null || !rangeMarker.isValid) return

        // Popup callbacks are live; the marker follows earlier replacements in the document.
        val document = rangeMarker.document
        val startOffset = rangeMarker.startOffset
        val endOffset = rangeMarker.endOffset
        if (startOffset < 0 || startOffset >= endOffset || endOffset > document.textLength) return

        val currentText = document.getText(TextRange(startOffset, endOffset))
        val replacement = formatReplacement(currentText, newColor)
        if (replacement == currentText) return

        WriteCommandAction.runWriteCommandAction(editor.project) {
          document.replaceString(startOffset, endOffset, replacement)
        }
      }
    }
  }

  override fun isNavigateAction(): Boolean = true

  override fun equals(other: Any?): Boolean {
    return when {
      this === other -> true
      other == null || javaClass != other.javaClass -> false
      else -> {
        val renderer = other as GutterColorRenderer
        color == renderer.color && range == renderer.range
      }
    }
  }

  override fun hashCode(): Int = Objects.hash(color, range)

  // IntelliJ lays out RIGHT-aligned renderers from right to left, reversing source order.
  override fun getAlignment(): Alignment = Alignment.LEFT

  companion object {
    private const val ICON_SIZE = 12
    private const val OPAQUE = 255
    private val HEX_COLOR = Regex("[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")

    private fun formatReplacement(original: String, color: Color): String {
      val quote = original.firstOrNull()?.takeIf { (it == '\'' || it == '\"') && original.lastOrNull() == it }
      val value = if (quote == null) original else original.substring(1, original.length - 1)
      val prefix = when {
        value.startsWith("#") -> "#"
        value.startsWith("0x") -> "0x"
        value.startsWith("0X") -> "0X"
        else -> ""
      }
      val digits = value.removePrefix(prefix)
      val formatted = when {
        HEX_COLOR.matches(digits) -> prefix + formatHex(color, digits)
        else -> (if (quote == null) "#" else "") + formatHex(color, "000000")
      }
      return if (quote == null) formatted else "$quote$formatted$quote"
    }

    private fun formatHex(color: Color, template: String): String {
      val rgb = "%02x%02x%02x".format(color.red, color.green, color.blue)
      val alpha = "%02x".format(color.alpha)
      var result = when (template.length) {
        8 -> rgb + alpha
        3 -> rgb.chunked(2).takeIf { pairs -> pairs.all { it.first() == it.last() } }
          ?.joinToString("") { it.first().toString() }
          ?: rgb
        else -> rgb
      }
      if (template.any(Char::isLetter) && template.filter(Char::isLetter).all(Char::isUpperCase)) {
        result = result.uppercase(Locale.ROOT)
      }
      return result
    }
  }
}
