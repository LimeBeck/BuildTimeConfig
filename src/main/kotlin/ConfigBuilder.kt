package dev.limebeck

import dev.limebeck.delegates.LiteralTemplateConfigPropertyDelegate
import dev.limebeck.delegates.NumberTemplateConfigPropertyDelegate
import dev.limebeck.delegates.ObjectConfigPropertyDelegate
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import java.io.File
import java.security.InvalidParameterException
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

open class ConfigBuilder(
    @Input
    val name: String? = null,
    objectFactory: ObjectFactory
) {

    @Input
    val packageName: Property<String> = objectFactory.property(String::class.java)

    @Input
    val objectName: Property<String> = objectFactory.property(String::class.java)

    @OutputDirectory
    val destination: RegularFileProperty = objectFactory.fileProperty()

    @Input
    val allProperties: MutableList<ConfigProperty> = mutableListOf()

    internal fun build(): Config {
        val name = name ?: "unnamed"
        return Config(
            configName = name,
            packageName = packageName.get(),
            objectName = objectName.get(),
            properties = allProperties,
            destinationDir = File(destination.get().asFile, name)
        )
    }

    @Suppress("UNUSED")
    fun configProperties(action: Action<ConfigPropertiesBuilder>) {
        val builder = ConfigPropertiesBuilder {
            action.execute(this)
        }
        allProperties.addAll(builder.allConfigProperties)
    }
}

open class ConfigPropertiesBuilder(initBlock: ConfigPropertiesBuilder.() -> Unit) {
    val allConfigProperties: MutableList<ConfigProperty> = mutableListOf()

    init {
        initBlock()
    }

    //Old style
    @Suppress("DEPRECATION")
    @Deprecated("Use new delegates API")
    fun <T : Any> property(name: String, type: KClass<T>, nullable: Boolean = false): ConfigPropertyDefinition<T> {
        return ConfigPropertyDefinition(name, type)
    }

    @Suppress("UNUSED", "DEPRECATION")
    @Deprecated("Use new delegates API")
    inline fun <reified T : Any> property(name: String, nullable: Boolean = false) = property(name, T::class, nullable)

    @Suppress("DEPRECATION")
    @Deprecated("Use new delegates API")
    infix fun <T : Any> ConfigPropertyDefinition<T>.set(value: T) {
        allConfigProperties.add(
            LiteralTemplateConfigProperty(
                name = name,
                type = type,
                value = value,
                nullable = false,
                template = when {
                    type.isSubclassOf(Boolean::class) -> "%L"
                    type.isSubclassOf(Number::class) -> "%L"
                    type.isSubclassOf(String::class) -> "%S"
                    else -> throw InvalidParameterException("<4ac3a89c> Unknown property type $type")
                }
            )
        )
    }

    @Suppress("UNUSED", "DEPRECATION")
    @Deprecated("Use new delegates API")
    fun obj(name: String) = ConfigObjectDefinition(name)

    @Suppress("DEPRECATION")
    @Deprecated("Use new delegates API")

    infix fun ConfigObjectDefinition.set(action: Action<ConfigPropertiesBuilder>) {
        val builder = ConfigPropertiesBuilder {
            action.execute(this)
        }
        allConfigProperties.add(ObjectConfigProperty(name, builder.allConfigProperties))
    }

    @Deprecated("Use new delegates API")
    data class ConfigPropertyDefinition<T : Any> internal constructor(
        val name: String,
        val type: KClass<T>
    )

    @Deprecated("Use new delegates API")
    data class ConfigObjectDefinition internal constructor(
        val name: String
    )


    //New style
    @Suppress("UNUSED")
    @JvmName("nullableString")
    fun string(value: String?) = LiteralTemplateConfigPropertyDelegate(
        value = value,
        type = String::class,
        template = "%S",
        configPropertiesBuilder = this
    )

    @Suppress("UNUSED")
    fun string(value: String) = LiteralTemplateConfigPropertyDelegate(
        value = value,
        type = String::class,
        template = "%S",
        configPropertiesBuilder = this
    )

    @Suppress("UNUSED")
    @JvmName("nullableBool")
    fun bool(value: Boolean?) = LiteralTemplateConfigPropertyDelegate(
        value = value,
        type = Boolean::class,
        template = "%L",
        configPropertiesBuilder = this
    )

    @Suppress("UNUSED")
    fun bool(value: Boolean) = LiteralTemplateConfigPropertyDelegate(
        value = value,
        type = Boolean::class,
        template = "%L",
        configPropertiesBuilder = this
    )

    @Suppress("UNUSED")
    @JvmName("nullableBoolean")
    fun boolean(value: Boolean?) = bool(value)

    @Suppress("UNUSED")
    fun boolean(value: Boolean) = bool(value)

    fun <T : Number?, R : T & Any> number(
        value: T,
        subtype: KClass<R>
    ) = NumberTemplateConfigPropertyDelegate(
        value = value,
        type = subtype,
        configPropertiesBuilder = this
    )

    @JvmName("nullableNumber")
    inline fun <reified T : Number> number(value: T?) = number(value, T::class)

    inline fun <reified T : Number> number(value: T) = number(value, T::class)

    @Suppress("UNUSED")
    @JvmName("nullableInt")
    fun int(value: Int?) = number(value)

    @Suppress("UNUSED")
    fun int(value: Int) = number(value)

    @Suppress("UNUSED")
    @JvmName("nullableLong")
    fun long(value: Long?) = number(value)

    @Suppress("UNUSED")
    fun long(value: Long) = number(value)

    @Suppress("UNUSED")
    @JvmName("nullableDouble")
    fun double(value: Double?) = number(value)

    @Suppress("UNUSED")
    fun double(value: Double) = number(value)

    @Suppress("UNUSED")
    @JvmName("nullableFloat")
    fun float(value: Float?) = number(value)

    @Suppress("UNUSED")
    fun float(value: Float) = number(value)

    @Suppress("UNUSED")
    fun obj(valueBuilder: ConfigPropertiesBuilder.() -> Unit) =
        ObjectConfigPropertyDelegate(valueBuilder, this)
}
