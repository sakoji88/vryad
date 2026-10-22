package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val size = 7 // поле 7x7
    private val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA)

    // board[строка][столбец] = номер цвета (индекс в colors), -1 = пустая клетка
    private val board = Array(size) { IntArray(size) }
    // View-шки клеток на экране
    private val cells = Array(size) { arrayOfNulls<View>(size) }

    // выбранная клетка (-1 = ничего не выбрано)
    private var selectedRow = -1
    private var selectedCol = -1

    private var score = 0
    private var moves = 20

    private lateinit var grid: GridLayout
    private lateinit var scoreText: TextView
    private lateinit var movesText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grid = findViewById(R.id.grid)
        scoreText = findViewById(R.id.scoreText)
        movesText = findViewById(R.id.movesText)
        findViewById<Button>(R.id.restartButton).setOnClickListener { newGame() }

        createCells()
        newGame()
    }

    // Создаём клетки-квадратики и кладём их в GridLayout
    private fun createCells() {
        grid.columnCount = size
        grid.rowCount = size
        val cellSize = resources.displayMetrics.widthPixels / (size + 1)

        for (r in 0 until size) {
            for (c in 0 until size) {
                val cell = View(this)
                val params = GridLayout.LayoutParams()
                params.width = cellSize
                params.height = cellSize
                params.setMargins(4, 4, 4, 4)
                cell.layoutParams = params
                cell.setOnClickListener { onCellClick(r, c) }
                grid.addView(cell)
                cells[r][c] = cell
            }
        }
    }

    // Новая игра: заполняем поле так, чтобы сразу не было готовых троек
    private fun newGame() {
        score = 0
        moves = 20
        selectedRow = -1
        selectedCol = -1

        for (r in 0 until size) {
            for (c in 0 until size) {
                var color: Int
                do {
                    color = Random.nextInt(colors.size)
                } while (
                    (c >= 2 && board[r][c - 1] == color && board[r][c - 2] == color) ||
                    (r >= 2 && board[r - 1][c] == color && board[r - 2][c] == color)
                )
                board[r][c] = color
            }
        }
        draw()
    }

    // Обработка нажатия на клетку
    private fun onCellClick(r: Int, c: Int) {
        if (moves == 0) return

        if (selectedRow == -1) {
            // ничего не выбрано — выбираем эту клетку
            selectedRow = r
            selectedCol = c
        } else if (selectedRow == r && selectedCol == c) {
            // нажали на ту же — снимаем выделение
            selectedRow = -1
            selectedCol = -1
        } else if (abs(selectedRow - r) + abs(selectedCol - c) == 1) {
            // нажали на соседнюю — пробуем поменять местами
            swap(selectedRow, selectedCol, r, c)
            if (findMatches().isEmpty()) {
                // троек нет — возвращаем обратно
                swap(selectedRow, selectedCol, r, c)
                Toast.makeText(this, "Нет совпадений", Toast.LENGTH_SHORT).show()
            } else {
                moves--
                processMatches()
            }
            selectedRow = -1
            selectedCol = -1
        } else {
            // нажали на далёкую клетку — просто выбираем её
            selectedRow = r
            selectedCol = c
        }

        draw()

        if (moves == 0) {
            Toast.makeText(this, "Игра окончена! Очки: $score", Toast.LENGTH_LONG).show()
        }
    }

    private fun swap(r1: Int, c1: Int, r2: Int, c2: Int) {
        val temp = board[r1][c1]
        board[r1][c1] = board[r2][c2]
        board[r2][c2] = temp
    }

    // Ищем все клетки, которые входят в 3+ одинаковых подряд
    private fun findMatches(): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()

        // по горизонтали
        for (r in 0 until size) {
            for (c in 0 until size - 2) {
                val v = board[r][c]
                if (v != -1 && v == board[r][c + 1] && v == board[r][c + 2]) {
                    result.add(Pair(r, c))
                    result.add(Pair(r, c + 1))
                    result.add(Pair(r, c + 2))
                }
            }
        }
        // по вертикали
        for (c in 0 until size) {
            for (r in 0 until size - 2) {
                val v = board[r][c]
                if (v != -1 && v == board[r + 1][c] && v == board[r + 2][c]) {
                    result.add(Pair(r, c))
                    result.add(Pair(r + 1, c))
                    result.add(Pair(r + 2, c))
                }
            }
        }
        return result
    }

    // Удаляем совпадения, роняем клетки вниз, досыпаем новые.
    // Повторяем, пока появляются новые тройки (каскады)
    private fun processMatches() {
        var matches = findMatches()
        while (matches.isNotEmpty()) {
            score += matches.size * 10
            for ((r, c) in matches) {
                board[r][c] = -1
            }
            dropDown()
            fillEmpty()
            matches = findMatches()
        }
    }

    // "Гравитация": в каждом столбце сдвигаем непустые клетки вниз
    private fun dropDown() {
        for (c in 0 until size) {
            var writeRow = size - 1
            for (r in size - 1 downTo 0) {
                if (board[r][c] != -1) {
                    board[writeRow][c] = board[r][c]
                    if (writeRow != r) board[r][c] = -1
                    writeRow--
                }
            }
        }
    }

    // Заполняем пустые клетки (они наверху) случайными цветами
    private fun fillEmpty() {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] == -1) {
                    board[r][c] = Random.nextInt(colors.size)
                }
            }
        }
    }

    // Перерисовываем поле по массиву board
    private fun draw() {
        for (r in 0 until size) {
            for (c in 0 until size) {
                val cell = cells[r][c]!!
                cell.setBackgroundColor(colors[board[r][c]])
                // выбранную клетку делаем полупрозрачной
                cell.alpha = if (r == selectedRow && c == selectedCol) 0.4f else 1f
            }
        }
        scoreText.text = "Очки: $score"
        movesText.text = "Ходы: $moves"
    }
}
