## Scala-BloomFilter

- Bloom过滤器用于成员存在性测试，它们速度快，空间效率高，但却以准确性为代价，虽然存在一定的错误概率，但是Bloom滤波器从不产生假负片
- Scala-bloomfilter是用Scala编写的一个独立的Bloom过滤器，实现它的目的是在没有额外库开销的情况下很容易地将其包含到现有项目中 
- 本项目主要用于本人后续爬虫项目[scala-akka-crawler](https://github.com/jxnu-liguobin/scala-akka-crawler)学习使用，仅供参考
- 假阳性是指因为某种原因把不具备某种特征的数据判断为具有某种特征的结果，即误判。但不会漏判具有某种特征的数据。（存在一定返回true，不存在也可能返回true）

参考

- [blog.locut.us](http://blog.locut.us/2008/01/12/a-decent-stand-alone-java-bloom-filter-implementation/)
- [布隆过滤器简洁介绍](https://blog.csdn.net/xinzhongtianxia/article/details/81294922)
- [大数据算法系列——布隆过滤器](https://www.cnblogs.com/zengdan-develpoer/p/4425167.html)
- [简书](https://www.jianshu.com/p/2104d11ee0a2)
- [csdn](https://blog.csdn.net/tianyaleixiaowu/article/details/74721877)

### 特性

* 支持序列化
* 您只需告诉它的构造函数您希望插入多少个元素，它就会自动配置自己进行最佳操作
* 包含相当彻底的单元测试
* 使用BitSet
* 提供计算预期假阳性率

### 环境

- Java 8
- Scala 2.11.8（为了兼容本人爬虫项目）
- Gradle

### 构建&使用

1. ```git clone https://github.com/jxnu-liguobin/Scala-BloomFilter```
2. ```cd Scala-BloomFilter/ ```
3. ```gradle clean build```
4. build目录下生成Scala-BloomFilter-1.0-SNAPSHOT.jar

要创建一个空的Bloom过滤器，只需使用所期望的假阳性概率和希望添加到Bloom过滤器中的元素个数来创建ScalaBloomFilter类的实例


```scala
    val elementCount: Int = 50000 // 测试元素个数
    val falsePositiveProbability: Double = 0.001 //假阳性概率
    val bf = new ScalaBloomFilter[String](falsePositiveProbability, elementCount)
```
* 构造函数选择一个哈希函数的长度和数目，这将提供给定的假阳性概率(大约)
* 请注意，如果插入的元素多于指定的预期元素数，则实际的假阳性概率将迅速增加
* 还有其他几个可用的构造函数，它们为Bloom过滤器的初始化提供了不同级别的控制。您还可以直接指定Bloom过滤器参数（每个元素的位数、散列函数和元素数）

在创建了Bloom过滤器之后，可以使用add()方法添加新元素
```scala
bf.add("test")
```

若要检查某个元素是否已存储在Bloom过滤器中，可以使用contains()方法
```scala
bf.contains("test"); // returns true
```

* 这种方法的准确性取决于假阳性概率
* 精度可以用预期的falsePositiveProbability()方法来估计
* 对于添加到Bloom过滤器的元素，它总是返回true，但对于尚未添加的元素，它也可能返回true

一个完整的例子

```scala
val elementCount: Int = 50000 // 测试元素个数
val falsePositiveProbability: Double = 0.001 //假阳性概率
val bf = new ScalaBloomFilter[String](falsePositiveProbability, elementCount)
bf.add("test")
if (bf.contains("test")) {
    println("存在元素: test")
    println("根据公式计算假阳性的预期概率: " + bf.expectedFalsePositiveProbability)
}
if (bf.contains("test1")) {
    System.out.println("There was a test1.")
}
```

### 相关问题

* HashMap 的问题

讲述布隆过滤器的原理之前，我们先思考一下，通常你判断某个元素是否存在用的是什么？应该蛮多人回答 HashMap 吧，确实可以将值映射到 HashMap 的 Key，然后可以在 O(1) 的时间复杂度内返回结果，效率奇高。
但是 HashMap 的实现也有缺点，例如存储容量占比高，考虑到负载因子的存在，通常空间是不能被用满的，而一旦你的值很多例如上亿的时候，那 HashMap 占据的内存大小就变得很可观了。
还比如说你的数据集存储在远程服务器上，本地服务接受输入，而数据集非常大不可能一次性读进内存构建 HashMap 的时候，也会存在问题。

    作者：YoungChen__
    链接：https://www.jianshu.com/p/2104d11ee0a2
    来源：简书
    简书著作权归作者所有，任何形式的转载都请联系作者获得授权并注明出处。
    
* 布隆过滤器数据结构    

布隆过滤器是一个 bit 向量或者说 bit 数组。

![](https://github.com/jxnu-liguobin/Scala-BloomFilter/tree/master/src/main/resources/error.jpg)
![](https://github.com/jxnu-liguobin/Scala-BloomFilter/tree/master/src/main/resources/hash.jpg)
![](https://github.com/jxnu-liguobin/Scala-BloomFilter/tree/master/src/main/resources/array.jpg)

* 为什么存在误判

随着增加的值越来越多，被置为 1 的 bit 位也会越来越多，这样某个值 “taobao” 即使没有被存储过，但是万一哈希函数返回的三个 bit 位都被其他值置位了 1 ，那么程序还是会判断 “taobao” 这个值存在。

* 为什么不支持删除

因为bit位被两个值共同覆盖的话，一旦你删除其中一个值而将其置位 0，那么下次判断另一个值例是否存在的话，会直接返回 false，而实际上你并没有删除它。

* 如何解决删除问题

计数删除。但是计数删除需要存储一个数值，而不是原先的 bit 位，会增大占用的内存大小。这样的话，增加一个值就是将对应索引槽上存储的值加一，删除则是减一，判断是否存在则是看值是否大于0。

* 如何选择哈希函数个数和布隆过滤器长度

过小的布隆过滤器很快所有的 bit 位均为 1，那么查询任何值都会返回“可能存在”，起不到过滤的目的了。布隆过滤器的长度会直接影响误报率，布隆过滤器越长其误报率越小。
另外，哈希函数的个数也需要权衡，个数越多则布隆过滤器 bit 位置位 1 的速度越快，且布隆过滤器的效率越低；但是如果太少的话，那我们的误报率会变高。

* 最佳实践

1. 大数据去重
2. 阻挡查询流量，减少磁盘io
3. 解决缓存穿透问题
4. 垃圾邮件过滤

* 大Value拆分

Redis 因其支持 setbit 和 getbit 操作，且纯内存性能高等特点，因此天然就可以作为布隆过滤器来使用。但是布隆过滤器的不当使用极易产生大 Value，增加 Redis 阻塞风险，因此生成环境中建议对体积庞大的布隆过滤器进行拆分。
拆分的形式方法多种多样，但是本质是不要将 Hash(Key) 之后的请求分散在多个节点的多个小 bitmap 上，而是应该拆分成多个小 bitmap 之后，对一个 Key 的所有哈希函数都落在这一个小 bitmap 上。

* 超过预期容量的情况解决方案

一般的解决思路是两个，一是存储原始数据，当 bf 超过 1000 个元素后生成一个 2000 个元素的 bf，另一种是堆叠 bf （叫做 scalable bloomfilter），超过 1000 个元素后再生成一个新的 1000 容量的 bf，查询的时候查多个。