package com.gio.bloomfilter

import java.util
import java.util.{BitSet, Collection}

import scala.collection.JavaConversions._

/**
 * Scala布隆过滤器
 *
 * 不支持协变
 *
 * @author 梦境迷离
 * @version 1.0, 2019-04-10
 * @param c 预期元素位数
 * @param n 预期最大元素个数
 * @param k 哈希函数个数
 * @tparam A 元素泛型的类型
 */
class ScalaBloomFilter[A](private val c: Double, private val n: Int, private val k: Int) extends Serializable {

    import ScalaBloomFilter._

    private var bitSetSize: Int = Math.ceil(c * n).toInt
    private var bitset: BitSet = new BitSet(bitSetSize)
    private val bitsPerElement: Double = c //equals/hascode忽略
    @volatile
    private var expectedNumberOfFilterElements: Int = n // 应添加（最多）个元素
    @transient
    private var numberOfAddedElements: Int = 0 // 实际添加到Bloom过滤器的元素数，equals/hascode忽略

    //标记是否达到了最大，不再需要扩容
    private var MAX_SIZE: Boolean = false
    private var reExpectedCount = 0

    /**
     * 构造一个空的Bloom过滤器哈希函数(K)的最优数目是根据Bloom的总大小和期望元素的数目来估计的
     *
     * @param bitSetSize              定义筛选器总共应该使用多少位
     * @param expectedNumberOElements 定义筛选器预期应包含的元素的最大数量
     */
    def this(bitSetSize: Int, expectedNumberOElements: Int) {
        this(bitSetSize / expectedNumberOElements.asInstanceOf[Double], expectedNumberOElements,
            //round四舍五入,log自然对数
            Math.round((bitSetSize / expectedNumberOElements.asInstanceOf[Double]) * Math.log(2.0)).asInstanceOf[Int])
    }

    /**
     * 构造具有给定假阳性概率的空Bloom滤波器估计每个元素的位数和散列函数的数目，以匹配假阳性概率
     *
     * @param falsePositiveProbability 期望的假阳性概率
     * @param expectedNumberOfElements Bloom过滤器中的预期元素数
     */
    def this(falsePositiveProbability: Double, expectedNumberOfElements: Int) {
        //ceil向上取整计算
        this(Math.ceil(-Math.log(falsePositiveProbability) / Math.log(2)) / Math.log(2), // c = k / ln(2)
            expectedNumberOfElements, Math.ceil(-Math.log(falsePositiveProbability) / Math.log(2)).toInt) // k = ceil(-log_2(false prob.))

    }

    /**
     * 在现有Bloom过滤器数据的基础上，构造了一种新的Bloom过滤器
     *
     * @param bitSetSize                     定义过滤器应使用的位数
     * @param expectedNumberOfFilterElements 定义筛选器预期应包含的元素的最大数量
     * @param actualNumberOfFilterElements   指定在<code>filterData</code>BitSet中插入了多少个元素（实际值）
     * @param filterData                     表示现有Bloom筛选器的BitSet
     */
    def this(bitSetSize: Int, expectedNumberOfFilterElements: Int, actualNumberOfFilterElements: Int, filterData: BitSet) {
        this(bitSetSize, expectedNumberOfFilterElements)
        this.bitset = filterData
        this.numberOfAddedElements = actualNumberOfFilterElements
    }

    /**
     * 使用默认的错误率 万分一
     *
     * @param expectedNumberOfFilterElements 定义筛选器预期应包含的元素的最大数量
     */
    def this(expectedNumberOfFilterElements: Int) {
        this(ScalaBloomFilter.FALSE_POSITIVE_PROBABILITY, expectedNumberOfFilterElements)
    }

    /**
     * 根据公式计算假阳性的预期概率
     *
     * 扩容时预期元素个数提升导致概率改变
     *
     * @return
     */
    def expectedFalsePositiveProbability: Double = {
        getFalsePositiveProbability(expectedNumberOfFilterElements)
    }

    /**
     * (1 - e^(-k * n / m)) ^ k
     *
     * @param numberOfElements
     * @return
     */
    def getFalsePositiveProbability(numberOfElements: Double): Double = {
        Math.pow(1 - Math.exp(-k * numberOfElements.asInstanceOf[Double] / bitSetSize.asInstanceOf[Double]), k)
    }

    /**
     * 得到当前出现假阳性的概率，概率由Bloom过滤器的大小和添加到其中的当前元素数计算
     *
     * @return
     */
    def getFalsePositiveProbability: Double = {
        getFalsePositiveProbability(numberOfAddedElements)
    }

    /**
     * K是基于Bloom过滤器的大小和插入元素的预期数的哈希函数的最佳数目
     *
     * @return
     */
    def getK: Int = this.k

    /**
     * 清空
     */
    def clear: Unit = {
        bitset.clear
        numberOfAddedElements = 0
    }

    /**
     * 将字节数组添加到Bloom筛选器中对象的toString()方法的输出用作哈希函数的输入
     *
     * @param bytes
     */
    def add(bytes: Array[Byte]): Unit = {
        val hashes: Array[Int] = createHashes(bytes, k)
        for (hash <- hashes) {
            val index = Math.abs(hash % bitSetSize)
            bitset.set(index, true)
        }
        numberOfAddedElements += 1
        try {
            //当实际元素大于预期元素的1.5时，进行扩容1.5倍
            val newSize = getExpectedNumberOfElements + getExpectedNumberOfElements.>>>(1)
            val level = getExpectedNumberOfElements.>>>(2) * 3
            if (!MAX_SIZE && numberOfAddedElements > level) {
                reExcepted(newSize, level)
            }
        } catch {
            case e: Exception => {
                //TODO 扩容失败
                //恢复
            }
        }
    }


    /**
     * 已知扩容次数
     *
     * @return
     */
    def getReExcepted(): Int = {
        this.reExpectedCount
    }

    /**
     * 调用此方法时表明预期元素的3/4小于当前实际元素个数，需要进行扩容重置预期元素，且此前未扩容至Int最大值。
     *
     * 此处简单实现第一种直接改变BF大小，并提升预期元素个数，未测试。
     *
     * 第二种扩容使用多个BF，查询时同时从多个BF中查询
     *
     * @param newSize
     * @param level
     */
    private def reExcepted(newSize: Int, level: Int) = {

        if (newSize < 0) {
            throw new RuntimeException("Capacity should not be less than 0")
        }
        //更新预期元素
        //大于倍则使用MaxValue
        this.expectedNumberOfFilterElements = if (newSize < Int.MaxValue - 8) newSize else {
            MAX_SIZE = true
            Int.MaxValue
        }
        //更新最大bit数
        val newBitSetSize = Math.ceil(c * expectedNumberOfFilterElements).toInt
        //创建更大的集合
        val newBitset = new util.BitSet(newBitSetSize)
        //newBitset = this.bitset.clone().asInstanceOf[util.BitSet] //底层使用复制替换，不能使用这种方法拷贝，否则size与前集合相同
        //拷贝索引值并覆盖原集合
        this.bitset = copyFrom(this.bitset, newBitset)
        this.bitSetSize = newBitSetSize
        //计算扩容次数
        reExpectedCount += 1
    }

    /**
     * 将集合中的所有元素添加到Bloom筛选器中
     *
     * @param c 元素集合 B必须是A的子类
     */
    def addAll[B <: A](c: Collection[B]): Unit = {
        for (element <- c.iterator()) {
            add(element)
        }
    }

    /**
     * 手动将原数据复制过来
     *
     * @param src
     * @param dest
     * @return
     */
    def copyFrom(src: util.BitSet, dest: util.BitSet): util.BitSet = {

        if (src == null || dest == null) {
            throw new RuntimeException("can't be null")
        }
        if (dest.size() < src.size()) {
            throw new RuntimeException("can't be less than src BitSet")
        }
        for (index <- 0 to src.size()) {
            if (src.get(index)) {
                dest.set(index, true)
            }
        }

        dest
    }

    /**
     * 将对象元素添加到Bloom筛选器中
     *
     * @param element 元素
     */
    def add(element: A): Unit = {
        add(element.toString.getBytes(charset))
    }

    /**
     * 如果元素可以插入到Bloom筛选器中，则返回true使用getFalsePositiveProbability()计算此正确的概率
     *
     * @param element 元素
     * @return 如果对象可以插入到Bloom过滤器中，则为true
     */
    def contains(element: A): Boolean = {
        contains(element.toString.getBytes(charset))
    }

    /**
     *
     * @param bytes 要检查的字节数组
     * @return 如果对象可以插入到Bloom过滤器中，则为true
     */
    def contains(bytes: Array[Byte]): Boolean = {
        val hashes = createHashes(bytes, k)
        for (hash <- hashes) {
            if (!bitset.get(Math.abs(hash % bitSetSize))) return false
        }
        true
    }

    /**
     * 如果集合的所有元素都可以插入到Bloom筛选器中，则返回true使用getFalsePositiveProbability()计算这一错误的概率
     *
     * @param c 要检查的元素 B必须是A的子类
     * @return 如果c中的所有元素都可以插入到Bloom过滤器中
     */
    def containsAll[B <: A](c: Collection[B]): Boolean = {
        //JavaConverters需要12.0
        for (element <- c) {
            if (!contains(element)) return false
        }
        true
    }

    /**
     * 从Bloom过滤器读取一个位
     *
     * @param bit the bit to read.
     * @return true if the bit is set, false if it is not.
     */
    def getBit(bit: Int): Boolean = {
        bitset.get(bit)
    }

    /**
     * 在Bloom过滤器中设置一个位集
     *
     * @param bit   要设置的位
     * @param value 如果为真，则设置位如果为false，则清除位
     */
    def setBit(bit: Int, value: Boolean): Unit = {
        bitset.set(bit, value)
    }

    /**
     * 返回用于存储Bloom过滤器的位集
     *
     * @return Bloom过滤器的位集
     */
    def getBitSet: BitSet = this.bitset

    /**
     * 返回Bloom筛选器中的位数使用count()检索插入元素的数量
     *
     * @return Bloom过滤器使用的位集的大小
     */
    def size: Int = this.bitSetSize

    /**
     * 返回在构建了之后或在调用了CLEAR()之后添加到Bloom过滤器的元素数
     *
     * @return 添加到Bloom筛选器中的元素数
     */
    def count: Int = this.numberOfAddedElements

    /**
     * 返回要插入到筛选器中的预期元素数此值与传递给构造函数的值相同
     *
     * @return 预期元素个数
     */
    def getExpectedNumberOfElements: Int = this.expectedNumberOfFilterElements

    /**
     * 当Bloom过滤器满时，获取每个元素的预期位数此值由构造函数在创建Bloom过滤器时设置还请参见getBitsPerElement()
     *
     * @return 每个元素的预期位数
     */
    def getExpectedBitsPerElement: Double = this.bitsPerElement

    /**
     * 根据当前插入的元素数和Bloom过滤器的长度，获取每个元素的实际位数还请参见getExpetedBitsPerElement()
     *
     * @return 每个元素的位数
     */
    def getBitsPerElement: Double = {
        this.bitSetSize / numberOfAddedElements.asInstanceOf[Double]
    }


    /**
     * 自定义的hashCode
     *
     * @return
     */
    override def hashCode(): Int = {
        //默认基数是31而不是61
        var hash = 7
        hash = 61 * hash + (if (this.bitset != null) this.bitset.hashCode else 0)
        hash = 61 * hash + this.expectedNumberOfFilterElements
        hash = 61 * hash + this.bitSetSize
        hash = 61 * hash + this.k
        hash
    }

    /**
     * 使用四个字段的equals方法
     *
     * @param obj
     * @return
     */
    override def equals(obj: scala.Any): Boolean = {
        if (obj == null) return false
        if (getClass ne obj.getClass) return false
        val other = obj.asInstanceOf[ScalaBloomFilter[A]]
        if (this.expectedNumberOfFilterElements != other.expectedNumberOfFilterElements) return false
        if (this.k != other.k) return false
        if (this.bitSetSize != other.bitSetSize) return false
        if ((this.bitset != other.bitset) && (this.bitset == null || !this.bitset.equals(other.bitset))) return false
        true
    }
}

object ScalaBloomFilter {

    import java.nio.charset.Charset
    import java.security.{MessageDigest, NoSuchAlgorithmException}

    //默认十万分一
    final lazy val FALSE_POSITIVE_PROBABILITY: Double = 0.0001

    private val charset: Charset = Charset.forName("UTF-8") // 用于将哈希值存储为字符串的编码
    private val hashName: String = "MD5" // MD5在大多数情况下具有足够的准确度如果需要的话换成sha1
    private var tmp: MessageDigest = _

    try {
        tmp = java.security.MessageDigest.getInstance(hashName)
    } catch {
        case e: NoSuchAlgorithmException =>
            tmp = null
    }
    private val digestFunction = tmp

    /**
     * * 根据字符串的内容生成摘要
     *
     * @param `val`
     * @param charset 编码
     * @return
     */
    def createHash(`val`: String, charset: Charset): Int = {
        createHash(`val`.getBytes(charset))
    }

    /**
     * 根据字符串的内容生成摘要
     *
     * @param `val`
     * @return
     */
    def createHash(`val`: String): Int = {
        createHash(`val`, charset)
    }

    /**
     * 根据字节数组的内容生成摘要
     *
     * @param data
     * @return
     */
    def createHash(data: Array[Byte]): Int = {
        createHashes(data, 1)(0)
    }


    /**
     * 根据字节数组的内容生成摘要，将结果拆分为4字节int，并将其存储在数组中
     * 摘要函数被调用时，直到生成所需的int数，对于每一个用于摘要的调用，都将SALT放在数据的前面，每次调用时SALT增加1。
     *
     * @param data   指定输入的数据
     * @param hashes 要产生的散列数/int数。
     * @return 整数hash数组
     */
    def createHashes(data: Array[Byte], hashes: Int): Array[Int] = {
        val result = new Array[Int](hashes)
        var k: Int = 0
        var salt: Byte = 0
        while (k < hashes) {
            var digest: Array[Byte] = null
            digestFunction synchronized {
                digestFunction.update(salt)
                salt = (salt.toInt + 1).toByte
                digest = digestFunction.digest(data)
            }
            for (i <- 0 until digest.length / 4 if k < hashes) {
                var h = 0
                for (j <- (i * 4) until (i * 4) + 4) {
                    h <<= 8
                    h |= digest(j).asInstanceOf[Int] & 0xFF
                }
                result(k) = h
                k += 1
            }
        }
        result
    }
}
