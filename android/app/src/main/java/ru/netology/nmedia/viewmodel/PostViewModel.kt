package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.postValue(FeedModel(loading = true))

        repository.getAllAsync(object : PostRepository.RepositoryCallback<List<Post>> {
            override fun onSuccess(value: List<Post>) {
                _data.postValue(FeedModel(posts = value, empty = value.isEmpty()))
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun save() {
        edited.value?.let {
            repository.saveAsync(it) { _postCreated.postValue(Unit) }
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long, likeRemoved: Boolean) {
        // Оптимистичная модель
        val oldPosts = _data.value?.posts.orEmpty()
        val updatedPosts = getPostsAfterLike(id, likeRemoved, oldPosts)
        _data.postValue(FeedModel(posts = updatedPosts))

        repository.likeByIdAsync(id, likeRemoved, object :
            PostRepository.RepositoryCallback<Post> {
            override fun onSuccess(value: Post) {
                // Данные успешно получены; обновляем пост на случай, если другие пользователи тоже ставили/убирали лайки
                val posts = _data.value?.posts.orEmpty().map { post ->
                    if (post.id == value.id) {
                        value
                    } else {
                        post
                    }
                }
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    private fun getPostsAfterLike(id: Long, likeRemoved: Boolean, oldPosts: List<Post>) =
        oldPosts.map { post ->
            if (post.id == id) {
                if (likeRemoved) {
                    post.copy(likedByMe = false, likes = post.likes - 1)
                } else {
                    post.copy(likedByMe = true, likes = post.likes + 1)
                }
            } else {
                post
            }
        }

    fun removeById(id: Long) {
        // Оптимистичная модель
        val old = _data.value?.posts.orEmpty()
        _data.postValue(
            _data.value?.copy(posts = _data.value?.posts.orEmpty()
                .filter { it.id != id }
            )
        )
        repository.removeByIdAsync(id) { _data.postValue(_data.value?.copy(posts = old)) }
    }
}