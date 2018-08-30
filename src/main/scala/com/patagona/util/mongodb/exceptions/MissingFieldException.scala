package com.patagona.util.mongodb.exceptions

case class MissingFieldException(fieldName: String, element: String)
    extends RuntimeException(s"Document does not contain field $fieldName: $element")
