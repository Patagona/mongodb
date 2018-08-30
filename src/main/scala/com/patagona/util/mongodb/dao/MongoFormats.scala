package com.patagona.util.mongodb.dao

import org.json4s.DefaultFormats
import org.json4s.ext.JodaTimeSerializers

object MongoFormats {
  val baseFormats = DefaultFormats.lossless ++ JodaTimeSerializers.all
  val baseFormatsWithMongoDateTime = baseFormats + MongoDateTimeSerializer()
}
