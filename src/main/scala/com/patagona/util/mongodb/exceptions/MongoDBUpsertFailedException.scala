package com.patagona.util.mongodb.exceptions

class MongoDBUpsertFailedException(message: String = "Upsert failed with unknown reason") extends Exception(message)
