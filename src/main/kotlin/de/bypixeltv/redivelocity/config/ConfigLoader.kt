package de.bypixeltv.redivelocity.config

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException

class ConfigLoader(private val configFilePath: String) {

    private val loaderOptions = LoaderOptions()
    private val yaml = Yaml(Constructor(Config::class.java, loaderOptions))
    var config: Config? = null
        private set

    init {
        load()
    }

    fun load() {
        val configFile = File(configFilePath)
        if (!configFile.parentFile.exists()) {
            configFile.parentFile.mkdirs()
        }
        if (!configFile.exists()) {
            try {
                configFile.createNewFile()
                config = Config()
                save()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            FileInputStream(configFile).use { inputStream ->
                config = yaml.load(inputStream)
            }
        }
    }

    private fun save() {
        FileWriter(configFilePath).use { writer ->
            writer.write("# Here you have to put the IP of your Redis database, if the Redis database is on the same Server as the proxy, you can ignore this setting\n")
            writer.write("redisHost: ${config?.redisHost}\n")
            writer.write("# Here you have to put the port of your Redis database, the default port is 6379\n")
            writer.write("redisPort: ${config?.redisPort}\n")
            writer.write("# Here you have to put the user name of your Redis database, the default user is named default\n")
            writer.write("redisUsername: ${config?.redisUsername}\n")
            writer.write("# This is where you have to put the password of your Redis database\n")
            writer.write("redisPassword: ${config?.redisPassword}\n")
            writer.write("# Here you can enable or disable the SSL connection to the Redis database\n")
            writer.write("useSsl: ${config?.useSsl}\n")
            writer.write("# Here you can put the channel for the Redis messages\n")
            writer.write("redisChannel: ${config?.redisChannel}\n")
            writer.write("# Here you can set the prefix for RediVelocity. For colorcodes you have to use minimessages\n")
            writer.write("prefix: ${config?.prefix}\n")
        }
    }
}