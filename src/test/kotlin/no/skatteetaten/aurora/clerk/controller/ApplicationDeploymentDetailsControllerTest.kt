package no.skatteetaten.aurora.clerk.controller

import no.skatteetaten.aurora.clerk.controller.security.BearerAuthenticationManager
import no.skatteetaten.aurora.clerk.service.PodService
import no.skatteetaten.aurora.clerk.service.openshift.token.UserDetailsProvider
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant

@WebMvcTest(
    ApplicationController::class,
    PodResourceAssembler::class,
    UserDetailsProvider::class,
    BearerAuthenticationManager::class
)
@AutoConfigureWebClient
@AutoConfigureRestDocs
class ApplicationDeploymentDetailsControllerTest : AbstractSecurityControllerTest() {

    @MockBean
    lateinit var podService: PodService

    val started = Instant.now().toString()
    val namespace = "jedi-test"
    val pods = listOf(PodItem("luke-1", "luke", started, "Running"))


    @Test
    @WithUserDetails
    fun `should get all pods in namespace`() {

        given(podService.getPodItems(namespace)).willReturn(pods)

        // TODO: kan dette skrives ved å sammenligne objektet over med noe her?
        mockMvc.perform(get("/api/pods/{namespace}", namespace))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].name", `is`("luke-1")))
            .andExpect(jsonPath("$.items[0].applicationName", `is`("luke")))
            .andExpect(jsonPath("$.items[0].startTime", `is`(started)))
            .andExpect(jsonPath("$.items[0].status", `is`("Running")))
            .andDo(document("home"))
    }
}