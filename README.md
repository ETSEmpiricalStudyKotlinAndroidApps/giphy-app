# Giphy app

- This application is made for android and is written in Kotlin.
- Model-View-ViewModel architectural pattern is used in this application.
- Request to the API are made with Retrofit with the help of coroutines. 
- Trending gifs are displayed in a grid on the home page
- Search is supported via by tapping on the magnifying glass icon
- Swtich to trending gifs from searched ones is done by tapping on the trending icon (fire)
- Online and offline mode are supported. During online mode, a list of trending gifs is saved on the internal storage, so that we can load it from the storage when internet connection is unavailable.
- Pagination is implemented for trending gifs, and also for the searched gifs in online mode.
- Gif can be loaded into fullscreen mode by just tapping on it.
- Pull to refresh is supported
- Gifs from the gallery can be uploaded by clicking on the FAB
- Landscape mode is also supported
