package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [SnippetEntity::class, ScanRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DevDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao
    abstract fun scanRecordDao(): ScanRecordDao

    companion object {
        @Volatile
        private var INSTANCE: DevDatabase? = null

        fun getDatabase(context: Context): DevDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevDatabase::class.java,
                    "dev_tools_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialSnippets(database.snippetDao())
                    }
                }
            }
        }

        private suspend fun populateInitialSnippets(dao: SnippetDao) {
            val now = System.currentTimeMillis()
            dao.insertSnippet(
                SnippetEntity(
                    title = "Quick Sort Algorithm",
                    language = "PYTHON",
                    code = """# Python Algorithm Demo
def quick_sort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quick_sort(left) + middle + quick_sort(right)

numbers = [64, 34, 25, 12, 22, 11, 90, 88, 45, 5]
print("Original Array:", numbers)
sorted_numbers = quick_sort(numbers)
print("Sorted Array:  ", sorted_numbers)
print("Array Length:  ", len(sorted_numbers))
print("Sum of items:  ", sum(sorted_numbers))
""".trimIndent(),
                    isFavorite = true,
                    lastModified = now
                )
            )

            dao.insertSnippet(
                SnippetEntity(
                    title = "Fibonacci Generator",
                    language = "JAVA",
                    code = """public class Main {
    public static void main(String[] args) {
        System.out.println("=== Java Runtime Execution ===");
        int n = 15;
        System.out.println("Generating first " + n + " Fibonacci numbers:");
        
        long first = 0, second = 1;
        for (int i = 1; i <= n; i++) {
            System.out.print(first + " ");
            long next = first + second;
            first = second;
            second = next;
        }
        System.out.println();
        System.out.println("Execution completed successfully.");
    }
}
""".trimIndent(),
                    isFavorite = true,
                    lastModified = now - 1000
                )
            )

            dao.insertSnippet(
                SnippetEntity(
                    title = "Memory Vector & Pointers",
                    language = "CPP",
                    code = """#include <iostream>
#include <vector>
#include <numeric>

using namespace std;

int main() {
    cout << "--- C++ Compiler Runtime (v17) ---" << endl;
    vector<int> data = {10, 20, 30, 40, 50};
    
    cout << "Vector elements: ";
    for (int num : data) {
        cout << num << " ";
    }
    cout << endl;
    
    int sum = 0;
    for (int val : data) {
        sum += val;
    }
    cout << "Computed Sum = " << sum << endl;
    cout << "Pointer offset testing: [data.front() = " << data.front() << "]" << endl;
    return 0;
}
""".trimIndent(),
                    isFavorite = false,
                    lastModified = now - 2000
                )
            )
        }
    }
}
