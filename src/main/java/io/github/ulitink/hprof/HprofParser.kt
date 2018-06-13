package io.github.ulitink.hprof

import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.logging.Logger

fun main(args: Array<String>) {
    LOG.info("STARTED")
    if (args.isEmpty()) throw IllegalArgumentException("Missing hprof file name")
    val file = File(args[0])
    if (!file.exists()) throw IOException("File not found")
    val loader = HprofLoader()
    HprofParser(DataInputStream(FileInputStream(file)), loader).parse()
    val applier = HprofApplier(loader)
    applier.loadHeapFromHprof()
}

private val LOG = Logger.getLogger(HprofParser::class.java.canonicalName)

class HprofParser(private val input: DataInputStream, private val consumer: RecordConsumer) {


    private var idSize = -1

    fun parse() {
        while (input.readByte() != 0.toByte());
        idSize = readU4()

        val highTime = readU4()
        val lowTime = readU4()

        while (input.available() > 0) {
            val tag = readU1()
            val time = readU4()
            val length = readU4()
            readRecord(tag, length)
        }
    }

    private fun readRecord(tag: u1, length: u4) {
        when (tag.toInt()) {
            0x01 -> {
                val id = readId()
                val bytes = ByteArray(length - idSize)
                input.readFully(bytes)
                consumer.stringInUTF8(id, bytes)
            }
            0x02 -> consumer.loadClass(readU4(), readId(), readU4(), readId())
            0x03 -> consumer.unloadClass(readU4())
            0x04 -> consumer.stackFrame(readId(), readId(), readId(), readId(), readU4(), readU4())
            0x05 -> {
                val stackTraceSerialNumber = readU4()
                val threadSerialNumber = readU4()
                val numberOfFrames = readU4()
                val frames = IdArray(numberOfFrames) { readId() }
                consumer.stackTrace(stackTraceSerialNumber, threadSerialNumber, frames)
            }
            0x06 -> {
                val flags = readU2()
                val cutoffRatio = readU4()
                val totalLiveBytes = readU4()
                val totalLiveInstances = readU4()
                val totalBytesAllocated = readU8()
                val totalInstancesAllocated = readU8()
                val numberOfSites = readU4()
                val sites = Array(numberOfSites) { AllocSite(readU1(), readU4(), readU4(), readU4(), readU4(), readU4(), readU4()) }
                consumer.allocSites(flags, cutoffRatio, totalLiveBytes, totalLiveInstances, totalBytesAllocated, totalInstancesAllocated, sites)
            }
            0x07 -> consumer.heapSummary(readU4(), readU4(), readU8(), readU8())
            0x0A -> consumer.startThread(readU4(), readId(), readU4(), readId(), readId(), readId())
            0x0B -> consumer.endThread(readU4())
            0x0C, 0x1C -> {
                var subtag = readU1()
                while (subtag.toInt() != 0x2C) {
                    readHeapDumpElement(subtag)
                    if (input.available() == 0) break
                    subtag = readU1()
                }
                // HEAP DUMP END
            }
            0x0D, 0x0E -> input.skipBytes(length)
            else -> LOG.warning("Unknown tag $tag")
        }
    }

    private fun readHeapDumpElement(subtag: u1) {
        when (subtag.toInt()) {
            0xFF -> consumer.rootUnknown(readId())
            0x01 -> consumer.rootJniGlobal(readId(), readId())
            0x02 -> consumer.rootJniLocal(readId(), readU4(), readU4())
            0x03 -> consumer.rootJavaFrame(readId(), readU4(), readU4())
            0x04 -> consumer.rootNativeStack(readId(), readU4())
            0x05 -> consumer.rootStickyClass(readId())
            0x06 -> consumer.rootThreadBlock(readId(), readU4())
            0x07 -> consumer.rootMonitorUsed(readId())
            0x08 -> consumer.rootThreadObject(readId(), readU4(), readU4())
            0x20 -> {
                val classObjectId = readId()
                val stackTraceSerialNumber = readU4()
                val superClassObjectId = readId()
                val classLoaderObjectId = readId()
                val signersObjectId = readId()
                val protectionDomainObjectId = readId()
                val reserved1 = readId()
                val reserved2 = readId()
                val instanceSize = readU4()
                val constants = Array(readU2().toInt()) {
                    val constantPoolIndex = readU2()
                    val typeOfEntry = getValueType(readU1())
                    ClassConstant(constantPoolIndex, typeOfEntry, readValue(typeOfEntry))
                }
                val staticFields = Array(readU2().toInt()) {
                    val id = readId()
                    val valueType = getValueType(readU1())
                    ClassStaticField(id, valueType, readValue(valueType))
                }
                val classInstanceFields = Array(readU2().toInt()) {
                    ClassInstanceField(readId(), getValueType(readU1()))
                }
                consumer.classDump(classObjectId,
                        stackTraceSerialNumber,
                        superClassObjectId,
                        classLoaderObjectId,
                        signersObjectId,
                        protectionDomainObjectId,
                        reserved1,
                        reserved2,
                        instanceSize,
                        constants,
                        staticFields,
                        classInstanceFields
                )
            }
            0x21 -> {
                val objectId = readId()
                val stackTraceSerialNumber = readU4()
                val classObjectId = readId()
                val fieldsSize = readU4()
                val fields = ByteArray(fieldsSize)
                input.readFully(fields)
                consumer.instanceDump(objectId, stackTraceSerialNumber, classObjectId, fields)
            }
            0x22 -> {
                val arrayObjectId = readId()
                val stackTraceSerialNumber = readU4()
                val numberOfElements = readU4()
                val arrayClassObjectId = readId()
                val elements = LongArray(numberOfElements) {
                    readId()
                }
                consumer.objectArrayDump(arrayObjectId, stackTraceSerialNumber, numberOfElements, arrayClassObjectId, elements)
            }
            0x23 -> {
                val arrayObjectId = readId()
                val stackTraceSerialNumber = readU4()
                val numberOfElements = readU4()
                val valueType = getValueType(readU1())
                val elements = Array(numberOfElements) {
                    readValue(valueType)
                }
                consumer.primitiveArrayDump(arrayObjectId, stackTraceSerialNumber, numberOfElements, valueType, elements)
            }
            else -> LOG.warning("Unknown heap dump element tag $subtag")
        }
    }

    private fun readId(): ID {
        if (idSize == 4) return input.readInt().toLong()
        if (idSize == 8) return input.readLong()
        throw InvalidHprofFileException("Size of 4 and 8 are supported")
    }

    private fun readU1(): u1 = input.readByte()
    private fun readU2(): u2 = input.readShort()
    private fun readU4(): u4 = input.readInt()
    private fun readU8(): u8 = input.readLong()

    private fun getValueType(basicType: u1): ValueBasicType = when (basicType.toInt()) {
        2 -> ValueBasicType.OBJECT
        4 -> ValueBasicType.BOOLEAN
        5 -> ValueBasicType.CHAR
        6 -> ValueBasicType.FLOAT
        7 -> ValueBasicType.DOUBLE
        8 -> ValueBasicType.BYTE
        9 -> ValueBasicType.SHORT
        10 -> ValueBasicType.INT
        11 -> ValueBasicType.LONG
        else -> {
            LOG.severe("Unknown type $basicType.")
            ValueBasicType.OBJECT
        }
    }

    private fun readValue(basicType: ValueBasicType): Any = basicType.readValue(input)
}

class InvalidHprofFileException(message: String? = null) : RuntimeException(message)