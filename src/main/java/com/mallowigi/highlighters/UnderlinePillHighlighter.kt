package com.mallowigi.highlighters

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.ColorUtil
import java.awt.Color

class UnderlinePillHighlighter : Highlighter {
  override fun getAttributesFlyweight(color: Color): TextAttributes {
    val attributes = TextAttributes()
    val background = EditorColorsManager.getInstance().globalScheme.defaultBackground
    val mix = ColorUtil.mix(background, color, color.alpha / 255.0)

    return TextAttributes.fromFlyweight(
      attributes.flyweight
        .withEffectType(EffectType.BOLD_DOTTED_LINE)
        .withEffectColor(mix))
  }
}
