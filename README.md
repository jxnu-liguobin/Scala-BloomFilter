## Scala-BloomFilter

- Bloom过滤器用于成员存在性测试，它们速度快，空间效率高，但却以准确性为代价，虽然存在一定的错误概率，但是Bloom滤波器从不产生假负片
- Scala-bloomfilter是用Scala编写的一个独立的Bloom过滤器，实现它的目的是在没有额外库开销的情况下很容易地将其包含到现有项目中，参考[blog.locut.us](http://blog.locut.us/2008/01/12/a-decent-stand-alone-java-bloom-filter-implementation/) 
- 本项目主要用于本人后续爬虫项目[scala-akka-crawler](https://github.com/jxnu-liguobin/scala-akka-crawler)学习使用，仅供参考
- 假阳性是指因为某种原因把不具备某种特征的数据判断为具有某种特征的结果，即误判。但不会漏判具有某种特征的数据

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