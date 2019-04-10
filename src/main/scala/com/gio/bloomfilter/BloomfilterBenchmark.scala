package com.gio.bloomfilter

import java.util

import scala.util.Random

/**
 * @author 梦境迷离
 * @version 1.0, 2019-04-10
 */
object BloomfilterBenchmark extends App {

    val elementCount: Int = 50000 // 测试元素个数
    val falsePositiveProbability: Double = 0.001 //假阳性概率
    val r = new Random
    val existingElements = new util.ArrayList[String](elementCount)
    for (i <- 0 until elementCount) {
        val b: Array[Byte] = new Array[Byte](200)
        r.nextBytes(b)
        existingElements.add(new String(b))
    }

    val nonExistingElements = new util.ArrayList[String](elementCount)
    for (i <- 0 until elementCount) {
        val b: Array[Byte] = new Array[Byte](200)
        r.nextBytes(b)
        nonExistingElements.add(new String(b))
    }

    //创建过滤器
    val bf = new ScalaBloomFilter[String](falsePositiveProbability, elementCount)
    println("Testing " + elementCount + " elements")
    println("k is " + bf.getK)

    //添加元素
    print("add(): ")

    //插入元素
    val start_add = System.currentTimeMillis
    for (i <- 0 until elementCount) {
        //add加泛型则必须与过滤器构造传入的泛型一致，否则省略
        bf.add(existingElements.get(i))
    }
    val end_add = System.currentTimeMillis
    printStat(start_add, end_add)

    // 存在时contains
    print("contains(), existing: ")
    val start_contains = System.currentTimeMillis
    for (i <- 0 until elementCount) {
        bf.contains(existingElements.get(i))
    }
    val end_contains = System.currentTimeMillis
    printStat(start_contains, end_contains)

    // 不存在时contains
    System.out.print("contains(), nonexisting: ")
    val start_ncontains = System.currentTimeMillis
    for (i <- 0 until elementCount) {
        bf.contains(nonExistingElements.get(i))
    }
    val end_ncontains = System.currentTimeMillis
    printStat(start_ncontains, end_ncontains)

    def printStat(start: Long, end: Long): Unit = {
        val diff = (end - start) / 1000.0
        System.out.println(diff + "s, " + (elementCount / diff) + " elements/s")
    }

    println("Test expectedFalsePositiveProbability")
    test

    def test(): Unit = {
        val bf = new ScalaBloomFilter[String](falsePositiveProbability, elementCount)
        bf.add("test")
        if (bf.contains("test")) {
            println("存在元素: test")
            println("根据公式计算假阳性的预期概率: " + bf.expectedFalsePositiveProbability)
        }
        if (bf.contains("test1")) {
            System.out.println("There was a test1.")
        }
    }

}
