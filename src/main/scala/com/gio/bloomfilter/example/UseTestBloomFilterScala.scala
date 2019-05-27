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
        //根据输入的预期元素个数计算而得，不变。（扩容后改变）
        println("根据公式计算假阳性的预期概率: " + bf.expectedFalsePositiveProbability)
        //根据当前已存入元素个数进行动态计算
        println("根据公式计算当前元素的假阳性概率: " + bf.getFalsePositiveProbability)
    }
    if (bf.contains("test1")) {
        println("There was a test1.")
    }
}
