package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): List<Post>
    fun likeById(id: Long, likeRemoved: Boolean): Post
    fun save(post: Post)
    fun removeById(id: Long)

    fun getAllAsync(callback: RepositoryCallback<List<Post>>)
    fun likeByIdAsync(id: Long, likeRemoved: Boolean, callback: RepositoryCallback<Post>)
    fun saveAsync(post: Post, callback: () -> Unit)
    fun removeByIdAsync(id: Long, errorCallback: () -> Unit)

    interface RepositoryCallback<T> {
        fun onSuccess(value: T)
        fun onError(e: Exception)
    }
}
