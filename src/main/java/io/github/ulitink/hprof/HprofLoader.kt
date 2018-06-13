package io.github.ulitink.hprof

class HprofLoader : RecordConsumer {

    val strings: MutableMap<ID, String> = mutableMapOf() //
    val classNames: MutableMap<ID, ID> = mutableMapOf() // class object ID -> string ID
    val classInstanceFields: MutableMap<ID, Array<ClassInstanceField>> = mutableMapOf()
    val objects: MutableMap<ID, Pair<ID, ByteArray>> = mutableMapOf()

    override fun stringInUTF8(id: ID, utf8chars: u1Array) {
        strings[id] = String(utf8chars)
    }

    override fun loadClass(serialNumber: u4, classObjectId: ID, stackTraceSerialNumber: u4, classNameStringId: ID) {
       classNames[classObjectId] = classNameStringId
    }

    override fun unloadClass(classSerialNumber: u4) {
    }

    override fun stackFrame(stackFrameId: ID, methodNameStringId: ID, methodSignatureStringId: ID, sourceFileNameStringId: ID, classSerialNumber: u4, lineNumber: u4) {
    }

    override fun stackTrace(stackTraceSerialNumber: u4, threadSerialNumber: u4, stackFrameIds: IdArray) {
    }

    override fun allocSites(flags: u2, cutoffRatio: u4, totalLiveBytes: u4, totalLiveInstances: u4, totalBytesAllocated: u8, totalInstancesAllocated: u8, allocSites: Array<AllocSite>) {
    }

    override fun heapSummary(liveBytes: u4, liveInstances: u4, bytesAllocated: u8, instancesAllocated: u8) {
    }

    override fun startThread(threadSerialNumber: u4, threadObjectId: ID, stackTraceSerialNumber: u4, threadNameStringId: ID, threadGroupNameId: ID, threadParentGroupNameId: ID) {
    }

    override fun endThread(threadSerialNumber: u4) {
    }

    override fun rootUnknown(objectId: ID) {
    }

    override fun rootJniGlobal(objectId: ID, jniGlobalRefId: ID) {
    }

    override fun rootJniLocal(objectId: ID, threadSerialNumber: u4, frameNumberInStackTrace: u4) {
    }

    override fun rootJavaFrame(objectId: ID, threadSerialNumber: u4, frameNumberInStackTrace: u4) {
    }

    override fun rootNativeStack(objectId: ID, threadSerialNumber: u4) {
    }

    override fun rootStickyClass(objectId: ID) {
    }

    override fun rootThreadBlock(objectId: ID, threadSerialNumber: u4) {
    }

    override fun rootMonitorUsed(objectId: ID) {
    }

    override fun rootThreadObject(threadObjectId: ID, threadSerialNumber: u4, stackTraceSerialNumber: u4) {
    }

    override fun classDump(classObjectId: ID, stackTraceSerialNumber: u4, superClassObjectId: ID, classLoaderObjectId: ID, signersObjectId: ID, protectionDomainObjectId: ID, reserved1: ID, reserved2: ID, instanceSize: u4, constants: Array<ClassConstant>, staticFields: Array<ClassStaticField>, instanceFields: Array<ClassInstanceField>) {
        classInstanceFields[classObjectId] = instanceFields
    }

    override fun instanceDump(objectId: ID, stackTraceSerialNumber: u4, classObjectId: ID, values: ByteArray) {
        objects[objectId] = Pair(classObjectId, values)
    }

    override fun objectArrayDump(arrayObjectId: ID, stackTraceSerialNumber: u4, numberOfElements: u4, arrayClassObjectId: ID, elements: IdArray) {
    }

    override fun primitiveArrayDump(arrayObjectId: ID, stackTraceSerialNumber: u4, numberOfElements: u4, elementType: ValueBasicType, elements: Array<Any>) {
    }
}