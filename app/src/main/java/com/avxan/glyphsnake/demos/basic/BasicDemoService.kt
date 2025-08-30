package com.avxan.glyphsnake.demos.basic

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothinglondon.sdkdemo.demos.GlyphMatrixService
import kotlinx.coroutines.*
import kotlin.random.Random

class BasicDemoService : GlyphMatrixService("Basic-Demo") {

    private lateinit var bgScope: CoroutineScope
    private var moveJob: Job? = null
    private var glyphManager: GlyphMatrixManager? = null

    private val gridWidth = 25
    private val gridHeight = 25
    private var direction = 1

    private var snake = mutableListOf(
        Pair(gridWidth / 2, gridHeight / 2),
        Pair(gridWidth / 2 - 1, gridHeight / 2),
        Pair(gridWidth / 2 - 2, gridHeight / 2)
    )
    private var food: Pair<Int, Int> = Pair(-1, -1)

    private val barrierPoints = listOf(
        Pair(0,0), Pair(1,0), Pair(2,0), Pair(3,0), Pair(4,0), Pair(5,0), Pair(6,0), Pair(7,0), Pair(8,0),
        Pair(0,1), Pair(1,1), Pair(2,1), Pair(3,1), Pair(4,1), Pair(5,1), Pair(6,1),
        Pair(0,2), Pair(1,2), Pair(2,2), Pair(3,2), Pair(4,2),
        Pair(0,3), Pair(1,3), Pair(2,3), Pair(3,3),
        Pair(0,4), Pair(1,4), Pair(2,4),
        Pair(0,5), Pair(1,5),
        Pair(0,6), Pair(1,6),
        Pair(0,7),
        Pair(0,8),
        Pair(16,0), Pair(17,0), Pair(18,0), Pair(19,0), Pair(20,0), Pair(21,0), Pair(22,0), Pair(23,0), Pair(24,0),
        Pair(18,1), Pair(19,1), Pair(20,1), Pair(21,1), Pair(22,1), Pair(23,1), Pair(24,1),
        Pair(20,2), Pair(21,2), Pair(22,2), Pair(23,2), Pair(24,2),
        Pair(21,3), Pair(22,3), Pair(23,3), Pair(24,3),
        Pair(22,4), Pair(23,4), Pair(24,4),
        Pair(23,5), Pair(24,5),
        Pair(23,6), Pair(24,6),
        Pair(24,7),
        Pair(24,8),
        Pair(0,16), Pair(0,17), Pair(0,18), Pair(1,18), Pair(0,19), Pair(1,19), Pair(0,20), Pair(1,20), Pair(2,20),
        Pair(0,21), Pair(1,21), Pair(2,21), Pair(3,21),
        Pair(0,22), Pair(1,22), Pair(2,22), Pair(3,22), Pair(4,22),
        Pair(0,23), Pair(1,23), Pair(2,23), Pair(3,23), Pair(4,23), Pair(5,23), Pair(6,23),
        Pair(0,24), Pair(1,24), Pair(2,24), Pair(3,24), Pair(4,24), Pair(5,24), Pair(6,24), Pair(7,24), Pair(8,24),
        Pair(24,16), Pair(24,17), Pair(23,18), Pair(24,18), Pair(23,19), Pair(24,19), Pair(22,20), Pair(23,20), Pair(24,20),
        Pair(21,21), Pair(22,21), Pair(23,21), Pair(24,21),
        Pair(20,22), Pair(21,22), Pair(22,22), Pair(23,22), Pair(24,22),
        Pair(18,23), Pair(19,23), Pair(20,23), Pair(21,23), Pair(22,23), Pair(23,23), Pair(24,23),
        Pair(16,24), Pair(17,24), Pair(18,24), Pair(19,24), Pair(20,24), Pair(21,24), Pair(22,24), Pair(23,24), Pair(24,24),
        Pair(15,24), Pair(14,24), Pair(13,24), Pair(12,24), Pair(11,24), Pair(10,24), Pair(9,24),
        Pair(17,23), Pair(16,23), Pair(8,23), Pair(7,23),
        Pair(19,22), Pair(18,22), Pair(6,22), Pair(5,22),
        Pair(20,21), Pair(4,21),
        Pair(21,20), Pair(3,20),
        Pair(22,19), Pair(2,19),
        Pair(22,18), Pair(2,18),
        Pair(23,17), Pair(1,17),
        Pair(23,16), Pair(1,16),
        Pair(24,15), Pair(0,15),
        Pair(24,14), Pair(0,14),
        Pair(24,13), Pair(0,13),
        Pair(24,12), Pair(0,12),
        Pair(24,11), Pair(0,11),
        Pair(24,10), Pair(0,10),
        Pair(24,9),  Pair(0,9),
        Pair(23,8),  Pair(1,8),
        Pair(23,7),  Pair(1,7),
        Pair(22,6),  Pair(2,6),
        Pair(22,5),  Pair(2,5),
        Pair(21,4),  Pair(3,4),
        Pair(20,3),  Pair(4,3),
        Pair(19,2),  Pair(18,2), Pair(6,2), Pair(5,2),
        Pair(17,1),  Pair(16,1), Pair(8,1), Pair(7,1),
        Pair(15,0),  Pair(14,0), Pair(13,0), Pair(12,0), Pair(11,0), Pair(10,0), Pair(9,0)
    )

    private var isGameOver = false
    private var isGameStarted = false
    private var hasPlayedOnce = false

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        Log.d("GlyphMatrix", "Service connected")
        this.glyphManager = glyphMatrixManager
        bgScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        prepareNewGame()
    }

    override fun performOnServiceDisconnected(context: Context) {
        super.performOnServiceDisconnected(context)
        this.glyphManager = null
        bgScope.cancel()
        moveJob?.cancel()
    }

    private fun startSnakeMovement(glyphMatrixManager: GlyphMatrixManager) {
        hasPlayedOnce = true
        moveJob?.cancel()
        moveJob = bgScope.launch {
            while (isActive && !isGameOver && isGameStarted) {
                moveSnake(glyphMatrixManager)
                drawSnake(glyphMatrixManager)
                delay(500L)
            }
            Log.d("Snake", "Main loop ended (isActive=${isActive}, isGameOver=$isGameOver)")
        }
    }

    private fun generateFood(): Pair<Int, Int> {
        val free = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                val p = Pair(x, y)
                if (!barrierPoints.contains(p) && !snake.contains(p)) {
                    free.add(p)
                }
            }
        }
        if (free.isEmpty()) {
            isGameOver = true
            return Pair(-1, -1)
        }
        return free[Random.nextInt(free.size)]
    }

    private fun moveSnake(glyphMatrixManager: GlyphMatrixManager) {
        if (isGameOver || !isGameStarted) return

        val head = snake.first()
        val newHead = when (direction) {
            0 -> Pair(head.first, head.second - 1)
            1 -> Pair(head.first + 1, head.second)
            2 -> Pair(head.first, head.second + 1)
            3 -> Pair(head.first - 1, head.second)
            else -> head
        }

        if (newHead.first !in 0 until gridWidth || newHead.second !in 0 until gridHeight) {
            gameOver(glyphMatrixManager)
            return
        }

        if (barrierPoints.contains(newHead) || snake.contains(newHead)) {
            gameOver(glyphMatrixManager)
            return
        }

        snake.add(0, newHead)

        if (newHead == food) {
            food = generateFood()
        } else {
            if (snake.size > 1) {
                snake.removeAt(snake.size - 1)
            }
        }
    }

    private suspend fun showGameOverAnimation(glyphMatrixManager: GlyphMatrixManager) {
        val whiteFrame = IntArray(gridWidth * gridHeight) { 2047 }

        runCatching { glyphMatrixManager.setMatrixFrame(whiteFrame) }
        delay(1000L)
    }

    private fun gameOver(glyphMatrixManager: GlyphMatrixManager) {
        if (isGameOver) return
        isGameOver = true
        isGameStarted = false
        moveJob?.cancel()
        Log.d("Snake", "Game Over triggered")

        bgScope.launch {
            try {
                showGameOverAnimation(glyphMatrixManager)
                prepareNewGame()
            } catch (t: Throwable) {
                Log.w("Snake", "gameOver animation failed: ${t.message}")
            }
        }
    }

    private fun prepareNewGame() {
        Log.d("Snake", "Preparing new game...")
        moveJob?.cancel()
        isGameOver = false
        isGameStarted = false
        snake = mutableListOf(
            Pair(gridWidth / 2, gridHeight / 2),
            Pair(gridWidth / 2 - 1, gridHeight / 2),
            Pair(gridWidth / 2 - 2, gridHeight / 2)
        )
        direction = 1
        food = generateFood()
        glyphManager?.let {
            showStartScreen(it)
        }
    }

    private val startImage = listOf(
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        "..XXX.XXX..XX..XXX..XXX..",
        ".X.....X..X..X.X..X..X...",
        "..XX...X..X..X.XXX...X...",
        "....X..X..XXXX.X.X...X...",
        ".XXX...X..X..X.X..X..X.X.",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        "........................."
    )

    private val gameOverImage = listOf(
        ".........................",
        ".........................",
        ".........................",
        ".........................",
        "..........XXXXX..........",
        "....XX..XXXXXXXXX........",
        "....XX.XXX.....XXX.......",
        "....XXXX.........XXX.....",
        "....XXX...........XX.....",
        "....XXXXXX.........XX....",
        "....XXXXXX..X......XX....",
        "...........X.X...........",
        ".............X...........",
        "............X............",
        "....XX.........XXXXXX....",
        "....XX......X..XXXXXX....",
        ".....XX...........XXX....",
        ".....XXX.........XXXX....",
        ".......XXX.....XXX.XX....",
        "........XXXXXXXXX..XX....",
        "..........XXXXX..........",
        ".........................",
        ".........................",
        ".........................",
        "........................."
    )

    private fun convertImageToArray(image: List<String>, brightness: Int): IntArray {
        val array = IntArray(gridWidth * gridHeight) { 0 }
        image.forEachIndexed { y, row ->
            row.forEachIndexed { x, char ->
                if (char == 'X') {
                    if (x in 0 until gridWidth && y in 0 until gridHeight) {
                        array[y * gridWidth + x] = brightness
                    }
                }
            }
        }
        return array
    }

    private fun showStartScreen(glyphMatrixManager: GlyphMatrixManager) {
        val imageToShow = if (!hasPlayedOnce) startImage else gameOverImage
        val frameArray = convertImageToArray(imageToShow, 2047)
        runCatching {
            glyphMatrixManager.setMatrixFrame(frameArray)
        }.onFailure {
            Log.w("Snake", "setMatrixFrame failed in showStartScreen: ${it.message}")
        }
    }

    private fun drawSnake(glyphMatrixManager: GlyphMatrixManager) {
        val array = IntArray(gridWidth * gridHeight) { 0 }

        barrierPoints.forEach { (x, y) ->
            if (x in 0 until gridWidth && y in 0 until gridHeight) {
                array[y * gridWidth + x] = 100
            }
        }

        snake.forEach { (x, y) ->
            if (x in 0 until gridWidth && y in 0 until gridHeight) {
                array[y * gridWidth + x] = 1500
            }
        }

        if (food.first in 0 until gridWidth && food.second in 0 until gridHeight) {
            array[food.second * gridWidth + food.first] = 2047
        }

        runCatching {
            glyphMatrixManager.setMatrixFrame(array)
        }.onFailure {
            Log.w("Snake", "setMatrixFrame failed in drawSnake: ${it.message}")
        }
    }

    override fun onTouchPointPressed() {
        if (isGameStarted && !isGameOver) {
            direction = (direction + 1) % 4
        }
    }

    override fun onTouchPointReleased() {
        if (!isGameStarted && !isGameOver) {
            isGameStarted = true
            isGameOver = false
            glyphManager?.let { startSnakeMovement(it) }
        }
    }
}
