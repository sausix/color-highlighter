package com.mallowigi.highlighters

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.ColorUtil
import com.intellij.ui.Gray
import java.awt.Color

class GlowHighlighter : Highlighter {
  override fun getAttributesFlyweight(color: Color): TextAttributes {
    val attributes = TextAttributes()
    val background = EditorColorsManager.getInstance().globalScheme.defaultBackground
    val mix = ColorUtil.mix(background, color, color.alpha / 255.0)

    return TextAttributes.fromFlyweight(
      attributes.flyweight
        .withBackground(mix)
        .withForeground(if (ColorUtil.isDark(mix)) Gray._254 else Gray._1))
  }
}
