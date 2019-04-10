package com.gio.bloomfilter.example

import com.gio.bloomfilter.ScalaBloomFilter

/**
 * @author 梦境迷离
 * @version 1.0, 2019-04-10
 */
object UseTestBloomFilterScala extends App {

    val elementCount: Int = 50000 // 测试元素个数
    val falsePositiveProbability: Double = 0.001 //假阳性概率
    val bf: ScalaBloomFilter[String] = new ScalaBloomFilter[String](falsePositiveProbability, elementCount)
    bf.add("test")
    if (bf.contains("test")) {
        println("存在元素: test")
        println("根据公式计算假阳性的预期概率: " + bf.expectedFalsePositiveProbability)
    }
    if (bf.contains("test1")) {
        println("There was a test1.")
    }
}
