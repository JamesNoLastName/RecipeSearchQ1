package com.example.recipesearchq1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RecipeFinderScreen() }
    }
}
data class Dish(val idMeal: String, val strMeal: String, val strMealThumb: String, val strInstructions: String)
data class DishResponse(val meals: List<Dish>?)
interface MealService {
    @GET("search.php")
    suspend fun findDishes(@Query("s") query: String): DishResponse
}
object NetworkClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val retrofit = Retrofit.Builder()
        // mealdb api
        .baseUrl("https://www.themealdb.com/api/json/v1/1/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: MealService = retrofit.create(MealService::class.java)
}
class RecipeFinderViewModel {
    private val _dishes = MutableStateFlow<List<Dish>?>(null)
    val dishes: StateFlow<List<Dish>?> = _dishes

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchDishes(query: String) {
        _dishes.value = null
        _error.value = null
        _loading.value = true

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.api.findDishes(query)
                _dishes.update { response.meals ?: emptyList() }
            } catch (e: Exception) {
                _error.update { e.message ?: "An error occurred" }
            } finally {
                _loading.update { false }
            }
        }
    }
}
@Composable
fun RecipeFinderScreen(viewModel: RecipeFinderViewModel = remember { RecipeFinderViewModel() }) {
    var searchInput by remember { mutableStateOf("") }
    val dishList by viewModel.dishes.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val errorText by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .background(Color(0xFFFFF8DC))
    ) {
        TextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            label = { Text("Find Recipes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.fetchDishes(searchInput) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Search")
        }

        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            errorText != null -> Text(errorText!!, textAlign = TextAlign.Center)
            dishList.isNullOrEmpty() -> Text("No results found", textAlign = TextAlign.Center)
            else -> LazyColumn { items(dishList!!) { DishItem(it) } }
        }
    }
}
@Composable
fun DishItem(dish: Dish) {
    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = dish.strMeal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Image(
                painter = rememberAsyncImagePainter(dish.strMealThumb),
                contentDescription = dish.strMeal,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = dish.strInstructions.take(100) + "...",
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            )
        }
    }
}
