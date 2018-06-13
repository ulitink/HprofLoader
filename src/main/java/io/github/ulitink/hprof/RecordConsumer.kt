package io.github.ulitink.hprof

import java.io.DataInputStream

typealias u1 = Byte
typealias u1Array = ByteArray
typealias u2 = Short
typealias u4 = Int
typealias u8 = Long
typealias ID = Long
typealias IdArray = LongArray

interface RecordConsumer {
    fun stringInUTF8(id: ID, utf8chars: u1Array)
    fun loadClass(serialNumber: u4, classObjectId: ID, stackTraceSerialNumber: u4, classNameStringId: ID)
    fun unloadClass(classSerialNumber: u4)
    fun stackFrame(stackFrameId: ID, methodNameStringId: ID, methodSignatureStringId: ID, sourceFileNameStringId: ID, classSerialNumber: u4, lineNumber: u4)
    fun stackTrace(stackTraceSerialNumber: u4, threadSerialNumber: u4, stackFrameIds: IdArray)
    fun allocSites(flags: u2, cutoffRatio: u4, totalLiveBytes: u4, totalLiveInstances: u4, totalBytesAllocated: u8, totalInstancesAllocated: u8, allocSites: Array<AllocSite>)
    fun heapSummary(liveBytes: u4, liveInstances: u4, bytesAllocated: u8, instancesAllocated: u8)
    fun startThread(threadSerialNumber: u4, threadObjectId: ID, stackTraceSerialNumber: u4, threadNameStringId: ID, threadGroupNameId: ID, threadParentGroupNameId: ID)
    fun endThread(threadSerialNumber: u4)

    fun rootUnknown(objectId: ID)
    fun rootJniGlobal(objectId: ID, jniGlobalRefId: ID)
    fun rootJniLocal(objectId: ID, threadSerialNumber: u4, frameNumberInStackTrace: u4)
    fun rootJavaFrame(objectId: ID, threadSerialNumber: u4, frameNumberInStackTrace: u4)
    fun rootNativeStack(objectId: ID, threadSerialNumber: u4)
    fun rootStickyClass(objectId: ID)
    fun rootThreadBlock(objectId: ID, threadSerialNumber: u4)
    fun rootMonitorUsed(objectId: ID)
    fun rootThreadObject(threadObjectId: ID, threadSerialNumber: u4, stackTraceSerialNumber: u4)

    fun classDump(classObjectId: ID,
                  stackTraceSerialNumber: u4,
                  superClassObjectId: ID,
                  classLoaderObjectId: ID,
                  signersObjectId: ID,
                  protectionDomainObjectId: ID,
                  reserved1: ID,
                  reserved2: ID,
                  instanceSize: u4,
                  constants: Array<ClassConstant>,
                  staticFields: Array<ClassStaticField>,
                  instanceFields: Array<ClassInstanceField>)

    fun instanceDump(objectId: ID,
                     stackTraceSerialNumber: u4,
                     classObjectId: ID,
                     values: ByteArray)

    fun objectArrayDump(arrayObjectId: ID,
                        stackTraceSerialNumber: u4,
                        numberOfElements: u4,
                        arrayClassObjectId: ID,
                        elements: IdArray)

    fun primitiveArrayDump(arrayObjectId: ID,
                           stackTraceSerialNumber: u4,
                           numberOfElements: u4,
                           elementType: ValueBasicType,
                           elements: Array<Any>)
}

data class AllocSite(val arrayIndicator: u1, val classSerialNumber: u4, val stackTraceSerialNumber: u4, val liveBytes: u4, val liveInstances: u4, val bytesAllocated: u4, val instancesAllocated: u4)
data class ClassConstant(val constantPoolIndex: u2, val typeOfEntry: ValueBasicType, val value: Any)
data class ClassStaticField(val nameStringId: ID, val typeOfField: ValueBasicType, val value: Any)
data class ClassInstanceField(val nameStringId: ID, val typeOfField: ValueBasicType)

enum class ValueBasicType {
    OBJECT,
    BOOLEAN,
    CHAR,
    FLOAT,
    DOUBLE,
    BYTE,
    SHORT,
    INT,
    LONG;

    fun readValue(input: DataInputStream): Any = when (this) {
        ValueBasicType.OBJECT -> input.readLong() // TODO fix for 32bit
        ValueBasicType.BOOLEAN -> input.readBoolean()
        ValueBasicType.CHAR -> input.readChar()
        ValueBasicType.FLOAT -> input.readFloat()
        ValueBasicType.DOUBLE -> input.readDouble()
        ValueBasicType.BYTE -> input.readByte()
        ValueBasicType.SHORT -> input.readShort()
        ValueBasicType.INT -> input.readInt()
        ValueBasicType.LONG -> input.readLong()
    }
}

