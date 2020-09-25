package edu.cs.skku.autosen_server

data class Data(val userId: String, val secs: Int, val data: HashMap<String, HashMap<String, HashMap<String, Float>>>)