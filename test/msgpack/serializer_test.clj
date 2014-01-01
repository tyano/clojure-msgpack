(ns msgpack.serializer-test
  (:require [clojure.test :refer :all]
            [msgpack.utils :refer :all]
            [msgpack.proto :refer :all]
            [msgpack.serializer :refer :all]))

(defmacro serializes-as [thing bseq]
  `(is (= (ubytes ~bseq) (seq (serialize ~thing)))))

(deftest nil-test
  (testing "nil"
    (serializes-as nil [0xc0])))

(deftest boolean-test
  (testing "booleans"
    (serializes-as false [0xc2])
    (serializes-as true [0xc3])))

(deftest int-test
  (testing "positive fixnum"
    (serializes-as 0 [0x00])
    (serializes-as 0x10 [0x10])
    (serializes-as 0x7f [0x7f]))
  (testing "negative fixnum"
    (serializes-as -1 [0xff])
    (serializes-as -16 [0xf0])
    (serializes-as -32 [0xe0]))
  (testing "uint 8"
    (serializes-as 0x80 [0xcc 0x80])
    (serializes-as 0xf0 [0xcc 0xf0])
    (serializes-as 0xff [0xcc 0xff]))
  (testing "uint 16"
    (serializes-as 0x100 [0xcd 0x01 0x00])
    (serializes-as 0x2000 [0xcd 0x20 0x00])
    (serializes-as 0xffff [0xcd 0xff 0xff]))
  (testing "uint 32"
    (serializes-as 0x10000 [0xce 0x00 0x01 0x00 0x00])
    (serializes-as 0x200000 [0xce 0x00 0x20 0x00 0x00])
    (serializes-as 0xffffffff [0xce 0xff 0xff 0xff 0xff]))
  (testing "uint 64"
    (serializes-as 0x100000000 [0xcf 0x00 0x00 0x00 0x01 0x00 0x00 0x00 0x00])
    (serializes-as 0x200000000000 [0xcf 0x00 0x00 0x20 0x00 0x00 0x00 0x00 0x00])
    (serializes-as 0xffffffffffffffff [0xcf 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff]))
  (testing "int 8"
    (serializes-as -33 [0xd0 0xdf])
    (serializes-as -100 [0xd0 0x9c])
    (serializes-as -128 [0xd0 0x80]))
  (testing "int 16"
    (serializes-as -129 [0xd1 0xff 0x7f])
    (serializes-as -2000 [0xd1 0xf8 0x30])
    (serializes-as -32768 [0xd1 0x80 0x00]))
  (testing "int 32"
    (serializes-as -32769 [0xd2 0xff 0xff 0x7f 0xff])
    (serializes-as -1000000000 [0xd2 0xc4 0x65 0x36 0x00])
    (serializes-as -2147483648 [0xd2 0x80 0x00 0x00 0x00]))
  (testing "int 64"
    (serializes-as -2147483649 [0xd3 0xff 0xff 0xff 0xff 0x7f 0xff 0xff 0xff])
    (serializes-as -1000000000000000002 [0xd3 0xf2 0x1f 0x49 0x4c 0x58 0x9b 0xff 0xfe])
    (serializes-as -9223372036854775808 [0xd3 0x80 0x00 0x00 0x00 0x00 0x00 0x00 0x00])))

(deftest float-test
  (testing "float 32"
    (serializes-as (float 0.0) [0xca 0x00 0x00 0x00 0x00])
    (serializes-as (float 2.5) [0xca 0x40 0x20 0x00 0x00]))
  (testing "float 64"
    (serializes-as 0.0 [0xcb 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00])
    (serializes-as 2.5 [0xcb 0x40 0x04 0x00 0x00 0x00 0x00 0x00 0x00])
    (serializes-as (Math/pow 10 35) [0xcb 0x47 0x33 0x42 0x61 0x72 0xc7 0x4d 0x82])))

(defn- fill-str [n c]
  (clojure.string/join "" (repeat n c)))

(deftest str-test
  (testing "fixstr"
    (serializes-as "hello world" [0xab 0x68 0x65 0x6c 0x6c 0x6f 0x20 0x77 0x6f 0x72 0x6c 0x64])
    (serializes-as "" [0xa0])
    (serializes-as "abc" [0xa3 0x61 0x62 0x63])
    (serializes-as (fill-str 31 \a) (cons 0xbf (repeat 31 0x61))))
  (testing "str 8"
    (serializes-as (fill-str 32 \b)
                   (concat [0xd9 0x20] (repeat 32 (byte \b))))
    (serializes-as (fill-str 100 \c)
                   (concat [0xd9 0x64] (repeat 100 (byte \c))))
    (serializes-as (fill-str 255 \d)
                   (concat [0xd9 0xff] (repeat 255 (byte \d)))))
  (testing "str 16"
    (serializes-as (fill-str 256 \b)
                   (concat [0xda 0x01 0x00] (repeat 256 (byte \b))))
    (serializes-as (fill-str 65535 \c)
                   (concat [0xda 0xff 0xff] (repeat 65535 (byte \c)))))
  (testing "str 32"
    (serializes-as (fill-str 65536 \b)
                   (concat [0xdb 0x00 0x01 0x00 0x00] (repeat 65536 (byte \b))))))

(deftest bin-test
  (testing "bin 8"
    (serializes-as (byte-array nil) [0xc4 0x00])
    (serializes-as (ubyte-array [0x80]) [0xc4 0x01 0x80])
    (serializes-as (ubyte-array (repeat 32 0x80)) (concat [0xc4 0x20] (repeat 32 0x80)))
    (serializes-as (ubyte-array (repeat 255 0x80)) (concat [0xc4 0xff] (repeat 255 0x80))))
  (testing "bin 16"
    (serializes-as (ubyte-array (repeat 256 0x80)) (concat [0xc5 0x01 0x00] (repeat 256 0x80))))
  (testing "bin 32"
    (serializes-as (ubyte-array (repeat 65536 0x80))
                   (concat [0xc6 0x00 0x01 0x00 0x00] (repeat  65536 0x80)))))

(declare reify-ext)

(deftest array-test
  (testing "fixarray"
    (serializes-as '() [0x90])
    (serializes-as [] [0x90])
    (serializes-as [[]] [0x91 0x90])
    (serializes-as [5 "abc", true] [0x93 0x05 0xa3 0x61 0x62 0x63 0xc3])
    (serializes-as [true 1 (reify-ext 3 (.getBytes "foo")) 0xff {1 false 2 "abc"} (ubyte-array [0x80]) [1 2 3] "abc"]
                   [0x98 0xc3 0x01 0xc7 0x03 0x03 0x66 0x6f 0x6f 0xcc 0xff 0x82 0x01 0xc2 0x02 0xa3 0x61 0x62 0x63 0xc4 0x01 0x80 0x93 0x01 0x02 0x03 0xa3 0x61 0x62 0x63]))
  (testing "array 16"
    (serializes-as (repeat 16 5)
                   (concat [0xdc 0x00 0x10] (repeat 16 5)))
    (serializes-as (repeat 65535 5)
                   (concat [0xdc 0xff 0xff] (repeat 65535 5))))
  (testing "array 32"
    (serializes-as (repeat 65536 5)
                   (concat [0xdd 0x00 0x01 0x00 0x00] (repeat 65536 5)))))

(deftest map-test
  (testing "fixmap"
    (serializes-as {} [0x80])
    (serializes-as {1 true 2 "abc" 3 (ubyte-array [0x80])}
                   [0x83 0x01 0xc3 0x02 0xa3 0x61 0x62 0x63 0x03 0xc4 0x01 0x80])
    (serializes-as {"abc" 5} [0x81 0xa3 0x61 0x62 0x63 0x05])
    (serializes-as {(ubyte-array [0x80]) 0xffff}
                   [0x81 0xc4 0x01 0x80 0xcd 0xff 0xff])
    (serializes-as {true nil} [0x81 0xc3 0xc0])
    (serializes-as {:compact true :schema 0}
                   [0x82 0xa6 0x73 0x63 0x68 0x65 0x6d 0x61 0x00 0xa7 0x63 0x6f 0x6d 0x70 0x61 0x63 0x74 0xc3])
    (serializes-as {1 [{1 2 3 4} {}], 2 1, 3 [false "def"], 4 {0x100000000 "a" 0xffffffff "b"}}
                   [0x84 0x01 0x92 0x82 0x01 0x02 0x03 0x04 0x80 0x02 0x01 0x03 0x92 0xc2 0xa3 0x64 0x65 0x66 0x04 0x82 0xcf 0x00 0x00 0x00 0x01 0x00 0x00 0x00 0x00 0xa1 0x61 0xce 0xff 0xff 0xff 0xff 0xa1 0x62]))
  (testing "map 16"
    (serializes-as (zipmap (range 0 16) (repeat 16 5))
                   (concat [0xde 0x00 0x10]
                           (interleave (range 0 16) (repeat 16 5))))))

(defn- reify-ext [type data]
  (reify Extension (ext-type [_] type) (ext-data [_] data)))

(deftest ext-test
  (testing "fixext 1"
    (serializes-as (reify-ext 5 [0x80]) [0xd4 0x05 0x80]))
  (testing "fixext 2"
    (serializes-as (reify-ext 5 [0x80 0x80]) [0xd5 0x05 0x80 0x80]))
  (testing "fixext 4"
    (serializes-as (reify-ext 5 [0x80 0x80 0x80 0x80])
                   [0xd6 0x05 0x80 0x80 0x80 0x80]))
  (testing "fixext 8"
    (serializes-as (reify-ext 5 [0x80 0x80 0x80 0x80 0x80 0x80 0x80 0x80])
                   [0xd7 0x05 0x80 0x80 0x80 0x80 0x80 0x80 0x80 0x80]))
  (testing "fixext 16"
    (serializes-as (reify-ext 5 (repeat 16 0x80))
                   (concat [0xd8 0x05] (repeat 16 0x80))))
  (testing "ext 8"
    (serializes-as (reify-ext 55 [0x1 0x3 0x11])
                   [0xc7 0x3 0x37 0x1 0x3 0x11])
    (serializes-as (reify-ext 5 (repeat 255 0x80))
                   (concat [0xc7 0xff 0x05] (repeat 255 0x80))))
  (testing "ext 16"
    (serializes-as (reify-ext 5 (repeat 256 0x80))
                   (concat [0xc8 0x01 0x00 0x05] (repeat 256 0x80))))
  (testing "ext 32"
    (serializes-as (reify-ext 5 (repeat 65536 0x80))
                   (concat [0xc9 0x00 0x01 0x00 0x00 0x05] (repeat 65536 0x80)))))
