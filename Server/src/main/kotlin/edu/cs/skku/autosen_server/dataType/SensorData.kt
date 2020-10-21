package edu.cs.skku.autosen_server.dataType

data class SensorData(val userId: String, val secs: Int, val data: HashMap<String, HashMap<String, HashMap<String, Float>>>)