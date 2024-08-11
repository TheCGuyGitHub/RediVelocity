package de.bypixeltv.redivelocity.managers

import de.bypixeltv.redivelocity.RediVelocity
import de.bypixeltv.redivelocity.config.Config
import redis.clients.jedis.BinaryJedisPubSub
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.json.JSONObject
import java.util.*

@Singleton
class RedisController @Inject constructor(
    private val rediVelocity: RediVelocity,
    config: Config
) : BinaryJedisPubSub(), Runnable {

    private val jedisPool: JedisPool
    private var channelsInByte: Array<ByteArray>
    private val isConnectionBroken = AtomicBoolean(true)
    private val isConnecting = AtomicBoolean(false)

    init {
        val jConfig = JedisPoolConfig()
        val maxConnections = 10

        jConfig.maxTotal = maxConnections
        jConfig.maxIdle = maxConnections
        jConfig.minIdle = 1
        jConfig.blockWhenExhausted = true

        val password = config.redis.password
        jedisPool = if (password.isEmpty()) {
            JedisPool(jConfig, config.redis.host, config.redis.port)
        } else {
            JedisPool(jConfig, config.redis.host, config.redis.port, 2000, password)
        }

        channelsInByte = setupChannels()
    }

    override fun run() {
        if (!isConnectionBroken.get() || isConnecting.get()) {
            return
        }
        rediVelocity.sendLogs("Connecting to Redis server...")
        isConnecting.set(true)
        try {
            jedisPool.resource.use { jedis ->
                isConnectionBroken.set(false)
                rediVelocity.sendLogs("Connection to Redis server has established! Success!")
                jedis.subscribe(this, *channelsInByte)
            }
        } catch (e: Exception) {
            isConnecting.set(false)
            isConnectionBroken.set(true)
            rediVelocity.sendErrorLogs("Connection to Redis server has failed! Please check your details in the configuration.")
            e.printStackTrace()
        }
    }

    fun shutdown() {
        jedisPool.close()
    }

    fun sendJsonMessage(event: String, proxyId: String, username: String, useruuid: String, clientbrand: String, userip: String, channel: String) {
        val jsonObject = JSONObject()
        jsonObject.put("action", event)
        jsonObject.put("proxyid", proxyId)
        jsonObject.put("username", username)
        jsonObject.put("uuid", useruuid)
        jsonObject.put("clientbrand", clientbrand)
        jsonObject.put("ipadress", userip)
        jsonObject.put("timestamp", System.currentTimeMillis())
        val jsonString = jsonObject.toString()

        // Publish the JSON string to the specified channel
        jedisPool.resource.use { jedis ->
            jedis.publish(channel, jsonString)
        }
    }

    fun sendJsonMessageSC(event: String, proxyId: String, username: String, useruuid: String, clientbrand: String, userip: String, serverName: String, previousServer: String, channel: String) {
        val jsonObject = JSONObject()
        jsonObject.put("action", event)
        jsonObject.put("proxyid", proxyId)
        jsonObject.put("username", username)
        jsonObject.put("uuid", useruuid)
        jsonObject.put("clientbrand", clientbrand)
        jsonObject.put("ipadress", userip)
        jsonObject.put("timestamp", System.currentTimeMillis())
        jsonObject.put("servername", serverName)
        jsonObject.put("previousserver", previousServer)

        val jsonString = jsonObject.toString()

        // Publish the JSON string to the specified channel
        jedisPool.resource.use { jedis ->
            jedis.publish(channel, jsonString)
        }
    }

    fun sendMessage(message: String, channel: String) {
        jedisPool.resource.use { jedis ->
            jedis.publish(channel, message)
        }
    }

    fun removeFromListByValue(listName: String, value: String) {
        jedisPool.resource.use { jedis ->
            jedis.lrem(listName, 0, value)
        }
    }

    fun setHashField(hashName: String, fieldName: String, value: String) {
        jedisPool.resource.use { jedis ->
            val type = jedis.type(hashName)
            if (type != "hash") {
                if (type == "none") {
                    jedis.hset(hashName, fieldName, value)
                } else {
                    System.err.println("Error: Key $hashName doesn't hold a hash. It holds a $type.")
                }
            } else {
                jedis.hset(hashName, fieldName, value)
            }
        }
    }

    fun deleteHashField(hashName: String, fieldName: String) {
        jedisPool.resource.use { jedis ->
            jedis.hdel(hashName, fieldName)
        }
    }

    fun deleteHash(hashName: String) {
        jedisPool.resource.use { jedis ->
            jedis.del(hashName)
        }
    }

    fun addToList(listName: String, values: Array<String>) {
        jedisPool.resource.use { jedis ->
            values.forEach { value ->
                jedis.rpush(listName, value)
            }
        }
    }

    fun setListValue(listName: String, index: Int, value: String) {
        jedisPool.resource.use { jedis ->
            val listLength = jedis.llen(listName)
            if (index >= listLength) {
                System.err.println("Error: Index $index does not exist in the list $listName.")
            } else {
                jedis.lset(listName, index.toLong(), value)
            }
        }
    }

    fun getHashValuesAsPair(hashName: String): Map<String, String> {
        val values = mutableMapOf<String, String>()
        jedisPool.resource.use { jedis ->
            val keys = jedis.hkeys(hashName)
            for (key in keys) {
                values[key] = jedis.hget(hashName, key)
            }
        }
        return values
    }

    fun removeFromList(listName: String, index: Int) {
        jedisPool.resource.use { jedis ->
            val listLength = jedis.llen(listName)
            if (index >= listLength) {
                System.err.println("Error: Index $index does not exist in the list $listName.")
            } else {
                val tempKey = UUID.randomUUID().toString()
                jedis.lset(listName, index.toLong(), tempKey)
                jedis.lrem(listName, 0, tempKey)
            }
        }
    }

    fun deleteList(listName: String) {
        jedisPool.resource.use { jedis ->
            jedis.del(listName)
        }
    }

    fun setString(key: String, value: String) {
        jedisPool.resource.use { jedis ->
            jedis.set(key, value)
        }
    }

    fun getString(key: String): String? {
        return jedisPool.resource.use { jedis ->
            jedis.get(key)
        }
    }

    fun deleteString(key: String) {
        jedisPool.resource.use { jedis ->
            jedis.del(key)
        }
    }

    fun getHashField(hashName: String, fieldName: String): String? {
        return jedisPool.resource.use { jedis ->
            jedis.hget(hashName, fieldName)
        }
    }

    fun getAllHashFields(hashName: String): Set<String>? {
        return jedisPool.resource.use { jedis ->
            jedis.hkeys(hashName)
        }
    }

    fun getAllHashValues(hashName: String): List<String>? {
        return jedisPool.resource.use { jedis ->
            jedis.hvals(hashName)
        }
    }

    fun getList(listName: String): List<String>? {
        return jedisPool.resource.use { jedis ->
            jedis.lrange(listName, 0, -1)
        }
    }

    fun getHashFieldNamesByValue(hashName: String, value: String): List<String> {
        val fieldNames = mutableListOf<String>()
        jedisPool.resource.use { jedis ->
            val keys = jedis.keys(hashName)
            for (key in keys) {
                val fieldsAndValues = jedis.hgetAll(key)
                for (entry in fieldsAndValues.entries) {
                    if (entry.value == value) {
                        fieldNames.add(entry.key)
                    }
                }
            }
        }
        return fieldNames
    }

    private fun setupChannels(): Array<ByteArray> {
        val channels = listOf("global", "messaging", "friends", "utils", "other") // replace with your actual channels
        return Array(channels.size) { channels[it].toByteArray(StandardCharsets.UTF_8) }
    }

    fun getJedisPool(): JedisPool {
        return jedisPool
    }
}