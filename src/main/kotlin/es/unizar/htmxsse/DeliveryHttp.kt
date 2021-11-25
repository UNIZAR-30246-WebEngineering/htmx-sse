package es.unizar.htmxsse

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.*

@Controller
class PdfGenerationController(
    private val sseRepository: SseRepository,
    private val pdfGenerator: PdfGenerator
) {

    @GetMapping
    fun index(model: Model): String {
        model["uuid"] = UUID.randomUUID().toString()
        return "index"
    }

    @PostMapping
    fun generatePdf(@RequestParam("uuid") id: String): String {
        val listener = sseRepository.createProgressListener(id)
        pdfGenerator.generatePdf(id, listener)
        return "index"
    }
}

