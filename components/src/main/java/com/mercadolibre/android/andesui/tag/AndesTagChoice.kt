package com.mercadolibre.android.andesui.tag

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.mercadolibre.android.andesui.R
import com.mercadolibre.android.andesui.tag.choice.AndesTagChoiceCallback
import com.mercadolibre.android.andesui.tag.choice.AndesTagChoiceState
import com.mercadolibre.android.andesui.tag.choice.AndesTagChoiceType
import com.mercadolibre.android.andesui.tag.factory.AndesChoiceTagConfigurationFactory
import com.mercadolibre.android.andesui.tag.factory.AndesTagChoiceAttrs
import com.mercadolibre.android.andesui.tag.factory.AndesTagChoiceAttrsParser
import com.mercadolibre.android.andesui.tag.factory.AndesTagChoiceConfiguration
import com.mercadolibre.android.andesui.tag.leftcontent.AndesTagLeftContent
import com.mercadolibre.android.andesui.tag.leftcontent.LeftContent
import com.mercadolibre.android.andesui.tag.rightcontent.AndesTagRightContent
import com.mercadolibre.android.andesui.tag.size.AndesTagSize
import com.mercadolibre.android.andesui.typeface.getFontOrDefault
import kotlinx.android.synthetic.main.andes_layout_simple_tag.view.*

class AndesTagChoice : ConstraintLayout {

    private lateinit var andesTagAttrs: AndesTagChoiceAttrs
    private lateinit var containerTag: ConstraintLayout

    /**
     * Getter and setter for [state].
     */
    var state: AndesTagChoiceState
        get() = andesTagAttrs.andesTagChoiceState
        set(value) {
            andesTagAttrs = andesTagAttrs.copy(andesTagChoiceState = value)
            setupComponents(createConfig())
        }

    /**
     * Getter and setter for [state].
     */
    var type: AndesTagChoiceType
        get() = andesTagAttrs.andesTagChoiceType
        set(value) {
            andesTagAttrs = andesTagAttrs.copy(andesTagChoiceType = value)
            setupRightContent(createConfig())
        }

    /**
     * Getter and setter for [text].
     */
    var text: String?
        get() = andesTagAttrs.andesSimpleTagText
        set(value) {
            andesTagAttrs = andesTagAttrs.copy(andesSimpleTagText = value)
            setupTitleComponent(createConfig())
        }

    /**
     * Getter and setter for [size].
     */
    var size: AndesTagSize
        get() = andesTagAttrs.andesTagSize
        set(value) {
            andesTagAttrs = andesTagAttrs.copy(andesTagSize = value)
            setupBackgroundComponents(createConfig())
            setupTitleComponent(createConfig())
        }

    private var callback: AndesTagChoiceCallback? = null

    /**
     * Getter and setter for [leftContent].
     */
    var leftContent: LeftContent?
        get() = andesTagAttrs.leftContentData
        set(value) {
            if (size == AndesTagSize.SMALL) {
                andesTagAttrs = andesTagAttrs.copy(leftContentData = null)
                andesTagAttrs = andesTagAttrs.copy(leftContent = null)
                Log.e("TAG", "LeftContent can only be used with tag large")
            } else {
                val andesTagLeftContent = when {
                    value?.dot != null -> AndesTagLeftContent.DOT
                    value?.icon != null -> AndesTagLeftContent.ICON
                    value?.image != null -> AndesTagLeftContent.IMAGE
                    else -> AndesTagLeftContent.NONE
                }
                andesTagAttrs = andesTagAttrs.copy(leftContentData = value)
                andesTagAttrs = andesTagAttrs.copy(leftContent = andesTagLeftContent)
            }

            val config = createConfig()
            setupLeftContent(config)
            setupTitleComponent(config)
        }

    @Suppress("unused")
    private constructor(context: Context) : super(context) {
        throw IllegalStateException(
                "Constructor without parameters in Andes Badge is not allowed. You must provide some attributes."
        )
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttrs(attrs)
    }

    @Suppress("unused")
    constructor(
            context: Context,
            type: AndesTagChoiceType = TYPE_DEFAULT,
            size: AndesTagSize = SIZE_DEFAULT,
            state: AndesTagChoiceState = STATE_DEFAULT,
            text: String? = TEXT_DEFAULT
    ) : super(context) {
        initAttrs(type, size, state, text)
    }

    /**
     * Sets the proper [config] for this message based on the [attrs] received via XML.
     * @param attrs attributes from the XML.
     */
    private fun initAttrs(attrs: AttributeSet?) {
        andesTagAttrs = AndesTagChoiceAttrsParser.parse(context, attrs)
        val config = AndesChoiceTagConfigurationFactory.create(andesTagAttrs)
        setupComponents(config)
    }

    private fun initAttrs(
            type: AndesTagChoiceType,
            size: AndesTagSize,
            state: AndesTagChoiceState,
            text: String?,
            leftContent: AndesTagLeftContent? = null,
            leftContentData: LeftContent? = null
    ) {
        andesTagAttrs = AndesTagChoiceAttrs(text, type, size, state, leftContentData, leftContent)
        val config = AndesChoiceTagConfigurationFactory.create(andesTagAttrs)
        setupComponents(config)
    }

    /**
     * Responsible for setting up all properties of each component that is part of this badge.
     * Is like a choreographer ;)
     */
    private fun setupComponents(config: AndesTagChoiceConfiguration) {
        initComponents()

        if (id == NO_ID) { // If this view has no id
            id = View.generateViewId()
        }

        setupTitleComponent(config)
        setupLeftContent(config)
        setupRightContent(config)
        setupBackgroundComponents(config)

        containerTag.setOnClickListener {
            onTagClick()
        }
    }

    private fun onTagClick() {
        val result = callback?.shouldSelectTag(this) ?: true
        if (result) {
            state = if (state == AndesTagChoiceState.SELECTED) {
                AndesTagChoiceState.IDLE
            } else {
                AndesTagChoiceState.SELECTED
            }
        }
    }

    /**
     * Creates all the views that are part of this badge.
     * After a view is created then a view id is added to it.
     */
    private fun initComponents() {
        val container = LayoutInflater.from(context).inflate(R.layout.andes_layout_simple_tag, this)
        containerTag = container.findViewById(R.id.andes_tag_container)

        // enable animations
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        containerTag.layoutTransition = layoutTransition
    }

    private fun setupBackgroundComponents(config: AndesTagChoiceConfiguration) {
        val shape = GradientDrawable()
        shape.cornerRadius = size.size.border(context)
        shape.setColor(config.backgroundColor.colorInt(context))
        val borderSize = resources.getDimension(R.dimen.andes_tag_border)
        shape.setStroke(borderSize.toInt(), config.borderColor.colorInt(context))
        background = shape

        containerTag.minHeight = size.size.height(context).toInt()
        containerTag.maxHeight = size.size.height(context).toInt()
        containerTag.minWidth = size.size.height(context).toInt()
    }

    /**
     * Gets data from the config and sets to the title component of this tag.
     */
    private fun setupTitleComponent(config: AndesTagChoiceConfiguration) {
        if (config.text == null || config.text.isEmpty()) {
            containerTag.visibility = View.GONE
        } else {
            containerTag.visibility = View.VISIBLE

            simpleTagText.text = config.text
            simpleTagText.typeface = context.getFontOrDefault(R.font.andes_font_regular)
            simpleTagText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.size.textSize(context))
            simpleTagText.setTextColor(config.textColor.colorInt(context))

            // TODO revisar margenes
            val constraintSet = ConstraintSet()
            constraintSet.clone(containerTag)
            if (leftContent == null) {
                constraintSet.setMargin(
                        R.id.simpleTagText,
                        ConstraintSet.START,
                        size.size.leftMargin(context)
                )
            } else if (config.leftContent != null) {
                constraintSet.setMargin(
                        R.id.simpleTagText,
                        ConstraintSet.START,
                        config.leftContent.content.rightMargin(context)
                )
            }
            if (config.rightContent == null || config.rightContent == AndesTagRightContent.NONE) {
                constraintSet.setMargin(R.id.simpleTagText, ConstraintSet.END, size.size.rightMargin(context))
            } else {
                constraintSet.setMargin(
                        R.id.simpleTagText,
                        ConstraintSet.END,
                        config.rightContent.content.rightMargin(context, size)
                )
            }

            constraintSet.applyTo(containerTag)
        }
    }

    private fun setupLeftContent(config: AndesTagChoiceConfiguration) {
        val leftContent = findViewById<FrameLayout>(R.id.leftContent)
        // TODO agregar margenes de la misma forma que sucede con el rightContent
        if (config.leftContent != null && config.leftContentData != null && config.leftContent != AndesTagLeftContent.NONE) {
            leftContent.removeAllViews()
            leftContent.addView(config.leftContent.content.view(context, config.leftContentData))
            leftContent.visibility = View.VISIBLE
        } else {
            leftContent.visibility = View.GONE
        }
    }

    private fun setupRightContent(config: AndesTagChoiceConfiguration) {
        val rightContent = findViewById<FrameLayout>(R.id.rightContent)
        if (config.rightContent != null && config.rightContent != AndesTagRightContent.NONE) {
            rightContent.removeAllViews()
            rightContent.addView(config.rightContent.content.view(
                    context,
                    config.rightContentColor,
                    null,
                    null
            ))

            val params = rightContent.layoutParams as MarginLayoutParams
            params.marginEnd = config.rightContent.content.rightMargin(context, size)
            rightContent.layoutParams = params

            rightContent.visibility = View.VISIBLE
        } else {
            rightContent.visibility = View.GONE
        }
    }

    private fun createConfig() = AndesChoiceTagConfigurationFactory.create(andesTagAttrs)

    companion object {
        private val TYPE_DEFAULT = AndesTagChoiceType.SIMPLE
        private val STATE_DEFAULT = AndesTagChoiceState.IDLE
        private val SIZE_DEFAULT = AndesTagSize.LARGE
        private val TEXT_DEFAULT = null
    }
}