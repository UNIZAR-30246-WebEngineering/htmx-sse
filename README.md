# Using Server-Sent Events with HTMX

It is possible to push information from a Spring Boot backend to the UI using Server-Sent Events.
This example will show how to use [HTMX](https://htmx.org/) to request a backend to push information to the UI with
Server-Sent Events. 

Think **HTMX** as an evolved way to use browser features from HTML rather than using javascript.
Combined with a server-side templating system such as **Thymeleaf**, it becomes a super-tool.

## The server side

To use SSE, we need to have a GET method that returns
a [SseEmitter](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html)
instance.

The SSE works like a channel where you push text events over it. The server should track the returned **SseEmitter**
instances so the server can decide where to send information. Not that the same client may open multiple SSE
connections.

The GET mapping is as follows:

```kotlin
@Controller
class SseController(
    private val repository: SseRepository
) {
    @GetMapping("/progress-events")
    fun progressEvents(@RequestParam("uuid") id: String): SseEmitter {
        val sseEmitter = SseEmitter(Long.MAX_VALUE)
        repository.put(id, sseEmitter)
        println("Adding SseEmitter for user: $id")
        with(sseEmitter) {
            onCompletion { LOGGER.info("SseEmitter for user $id is completed") }
            onTimeout { LOGGER.info("SseEmitter for user $id is timed out") }
            onError { ex -> LOGGER.info("SseEmitter for user $id got error:", ex) }
        }
        return sseEmitter
    }

    companion object {
        private val LOGGER by logger()
    }
}
```

We also need a POST method that triggers a simulated long action:

```kotlin
@PostMapping
fun generatePdf(@RequestParam("uuid") id: String): String {
    val listener = sseRepository.createProgressListener(id)
    pdfGenerator.generatePdf(id, listener)
    return "index"
}
```

The process do something random for a long time and uses the listener to report to the user:

```kotlin
interface ProgressListener {
    fun onProgress(value: Int)
    fun onCompletion()
}
```

The following implementation fo the listener has a reference to the **SseEmitter** and reports the progress to the user:

```kotlin
class SseEmitterProgressListener(private val sseEmitters: Collection<SseEmitter>) : ProgressListener {
    override fun onProgress(value: Int) {
        val html = """
            <div id="progress-container" class="progress-container">
                <div class="progress-bar" style="width:$value%"></div>
            </div>
            """.trimIndent()
        sendToAllClients(html)
    }

    override fun onCompletion() {
        val html = "<div><a href=\"#\">Download PDF</div>"
        sendToAllClients(html)
    }

    private fun sendToAllClients(html: String) {
        for (sseEmitter in sseEmitters) {
            try {
                // multiline strings are sent as multiple "data:" SSE
                // this confuses htmx in our example so, we remove all newlines
                // so only one "data:" is sent per html
                sseEmitter.send(html.replace("\n", ""))
            } catch (ex: IOException) {
                LOGGER.error(ex.message, ex)
            }
        }
    }

    companion object {
        private val LOGGER by logger()
    }
}
```

## The client side

This is the HTML template (```index.html```) that has a button to start the simulated long process:

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
<head>
    <meta charset="UTF-8">
    <title>Title</title>
    <link rel="stylesheet" href="/css/application.css">
</head>
<body>
<h1>Server Sent Events Demo</h1>
<div hx-sse="" th:attr="hx-sse='connect:/progress-events?uuid='+${uuid}">
    <button
            hx-post="/"
            hx-include="[name='uuid']"
            hx-swap="none">Generate PDF
    </button>
    <input name="uuid" type="hidden" value="" th:attr="value=${uuid}">
    <div style="margin-bottom: 2rem;"></div>
    <div id="progress-wrapper" hx-sse="swap:message">
    </div>
</div>
<script type="text/javascript" th:src="@{/webjars/htmx.org/dist/htmx.min.js}"></script>
</body>
</html>
```

Thymeleaf (```th:```) is used here to inject a unique user id (```uuid```) that will be attached to each user requests.
This will be helpful to detect which SSE connection in the pool should be used for reporting progress.

HTMX attributes (```hx-sse, hx-post, hx-include, hx-swap```) enable:

- To open an SSE connection when the HTML page is loaded by the browser (```hx-sse="connect:/progress-events"```).
- To show the content of the SSE events in the DOM (```hx-sse="swap:message"```).
- To link the action of a button with a POST request (```hx-post```) that will include in the request parameters found
  in other parts of the HTML (```hx-include```).

## Conclusion

This code is an example that modern libraries simplify the development of web applications.

This code is a Kotlin adaptation and simplification of the post [Using Server-Sent Events with Thymeleaf and HTMX
](https://www.wimdeblauwe.com/blog/2021/11/23/using-server-sent-events-with-thymeleaf-and-htmx/) of [Wim Deblauwe](https://www.wimdeblauwe.com/). 