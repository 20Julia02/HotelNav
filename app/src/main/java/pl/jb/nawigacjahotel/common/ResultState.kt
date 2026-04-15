package pl.jb.nawigacjahotel.common

sealed class ResultState<out T> {
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val throwable: Throwable) : ResultState<Nothing>()
    object Loading : ResultState<Nothing>()
}