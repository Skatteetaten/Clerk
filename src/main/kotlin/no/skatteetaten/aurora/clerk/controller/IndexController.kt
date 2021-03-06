package no.skatteetaten.aurora.clerk.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping(value = ["/"])
class IndexController {

    @GetMapping()
    fun index(): String {
        return "redirect:/docs/index.html"
    }
}
