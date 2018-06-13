cd ./build/classes/java/test/
java TestDataGenerator &
TEST_PROCESS=$!
cd -
rm src/test/hprofs/*
jmap -dump:live,format=b,file=src/test/hprofs/field_values.hprof $TEST_PROCESS
kill $TEST_PROCESS

