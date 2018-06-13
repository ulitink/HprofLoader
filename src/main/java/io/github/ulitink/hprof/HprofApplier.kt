package io.github.ulitink.hprof

import com.sun.istack.internal.logging.Logger
import org.objenesis.ObjenesisStd
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

class HprofApplier(private val hprof: HprofLoader) {
    companion object {
        private val OBJENESIS_STD = ObjenesisStd()
        private val MODIFIERS_FIELD = Field::class.java.getDeclaredField("modifiers")

        init {
            MODIFIERS_FIELD.isAccessible = true
        }
    }

    val instances = HashMap<ID, Any?>()

    fun loadHeapFromHprof() {
        hprof.objects.forEach { t, u ->
            instances[t] = createObject(u.first)
        }
        hprof.objects.forEach { t, u ->
            instances[t]?.let {
                fillData(it, u.first, u.second)
            }
        }
    }

    private fun createObject(classObjectId: ID): Any? {
        try {
            val classNameId = hprof.classNames[classObjectId]
                    ?: throw InvalidHprofFileException("Class name for $classObjectId wasn't found")
            val classPath = hprof.strings[classNameId]
                    ?: throw InvalidHprofFileException("String for $classNameId wasn't found")
            val aClass = Class.forName(classPath.replace('/', '.'))
            @Suppress("UnnecessaryVariable")
            val instance = OBJENESIS_STD.newInstance(aClass)
            return instance
        }
        catch (e: Throwable) {
            Logger.getLogger(javaClass).severe(e.message)
        }

        return null
    }

    private fun fillData(obj: Any, classObjectId: ID, fieldValues: ByteArray) {
        val objectClass = obj.javaClass
        val instanceFields = hprof.classInstanceFields[classObjectId]
                ?: throw InvalidHprofFileException("Fields for class $classObjectId wasn't found")

        val fieldValuesStream = DataInputStream(ByteArrayInputStream(fieldValues))
        for (field in instanceFields) {
            val fieldNameStringId = field.nameStringId
            val fieldName = hprof.strings[fieldNameStringId]
            val declaredField: Field = objectClass.getDeclaredField(fieldName)
            declaredField.isAccessible = true
            MODIFIERS_FIELD.setInt(declaredField, declaredField.modifiers and Modifier.FINAL.inv())

            val instanceFieldValue = field.typeOfField.readValue(fieldValuesStream)
            val value =
                    if (field.typeOfField == ValueBasicType.OBJECT) this.instances[instanceFieldValue as ID]
                    else instanceFieldValue

            declaredField.set(obj, value)
        }
    }

}
