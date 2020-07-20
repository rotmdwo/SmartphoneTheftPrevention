package edu.skku.cs.autosen

import android.os.Handler
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun thread_name_test() {
        print(Thread.currentThread().name)

        val handler = Handler()
        handler.post {
            print(Thread.currentThread().name)
        }
    }
}
