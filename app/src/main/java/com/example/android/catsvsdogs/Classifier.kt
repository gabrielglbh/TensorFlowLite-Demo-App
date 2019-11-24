package com.example.android.catsvsdogs

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.round

class Classifier(private val ctx: Context) {

    // TODO: Change INPUT_WIDTH and INPUT_HEIGHT to match your input tensor on the model
    private val INPUT_WIDTH = 75
    private val INPUT_HEIGHT = 75
    // TODO: Change the FloatArray size to match the number of labels of your model
    private val NUM_CLASSES = 2

    /**
     * loadModel: Loads the tensorflow lite model based on model
     * @return Interpreter for the model or null
     */
    fun loadModel(): Interpreter? {
        try {
            val assetManager = ctx.assets
            // TODO: Change the model for yours here
            val fd = assetManager.openFd("model.tflite")
            val fis = FileInputStream(fd.fileDescriptor)

            val channel = fis.channel
            val offset = fd.startOffset
            val length = fd.declaredLength

            val tfLiteModel = channel.map(FileChannel.MapMode.READ_ONLY, offset, length) as ByteBuffer
            return Interpreter(tfLiteModel)
        } catch (err: FileNotFoundException) {
            Toast.makeText(ctx, "Error loading the model", Toast.LENGTH_LONG).show()
        }
        return null
    }

    /**
     * evaluate: Runs the model with the bitmap bm, resizes the bitmap to 75x75
     * @param bm: Bitmap containing the drawn image
     * @param interpreter: Reference to the interpreter of the model created by loadModel()
     * @return an array with the predicted label and level of confidence
     */
    fun evaluate(interpreter: Interpreter?, bm: Bitmap): Array<String> {
        val img = Bitmap.createScaledBitmap(bm, INPUT_WIDTH, INPUT_HEIGHT, true)
        val buffer = convertBitmapToByteBuffer(img)

        val result = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter?.run(buffer, result)
        val parsedResults = ArrayList<Float>()
        for (res in result[0]) {
            parsedResults.add(res)
        }

        val conf = Collections.max(parsedResults)
        val indexOfMax = parsedResults.indexOf(conf)
        val confidence = round(conf * 100).toString() +  "%"
        return arrayOf(MainActivity.classes[indexOfMax], confidence)
    }

    /**
     * convertBitmapToByteBuffer: Gathers the image of the painted character with the desired
     * height and width and creates a ByteBuffer of the image with the red, green and blue channels
     * already normalized
     * @param bm: Bitmap representing the image
     * @return ByteBuffer containing an image with the three channels normalized
     */
    private fun convertBitmapToByteBuffer(bm: Bitmap): ByteBuffer {
        val BATCH_SIZE = 4
        val PIXEL_SIZE = 3

        val imageMean = 0f
        val imageStd = 255.0f

        val bimg =
            ByteBuffer.allocateDirect(BATCH_SIZE * INPUT_WIDTH * INPUT_HEIGHT * PIXEL_SIZE)
        bimg.order(ByteOrder.nativeOrder())

        val values = IntArray(INPUT_WIDTH * INPUT_HEIGHT)
        bm.getPixels(values, 0, bm.width, 0, 0, bm.width, bm.height)

        var pixel = 0
        for (x in 0 until INPUT_WIDTH) {
            for (y in 0 until INPUT_HEIGHT) {
                val input = values[pixel++]

                val r = ((input and 0xFF) - imageMean) / imageStd
                val g = ((input shr 8 and 0xFF) - imageMean) / imageStd
                val b = ((input shr 16 and 0xFF) - imageMean) / imageStd

                bimg.putFloat(r)
                bimg.putFloat(g)
                bimg.putFloat(b)
            }
        }
        return bimg
    }
}