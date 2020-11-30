package no.skatteetaten.aurora.clerk.controller

import mu.KotlinLogging
import no.skatteetaten.aurora.clerk.service.DeploymentConfigService
import no.skatteetaten.aurora.clerk.service.PodService
import no.skatteetaten.aurora.clerk.service.openshift.token.UserDetailsProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientResponseException

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class ApplicationController(
    val userProvider: UserDetailsProvider,
    val deploymentConfigService: DeploymentConfigService,
    val podService: PodService
) {

    @PutMapping("/scale/{namespace}")
    fun scale(
        @PathVariable namespace: String,
        @RequestParam("sleep", defaultValue = "500") sleep: Long,
        @RequestBody command: ScaleCommand
    ): ClerkResponse<ScaleResult> {
        validateUser(namespace)
        try {
            val scaleResult = deploymentConfigService.scale(command, namespace, sleep)
            logger.info("Scaling dc=${command.name} to replicas=${command.replicas} in namespace=$namespace")
            return ClerkResponse(items = listOf(scaleResult), message = "Scaled applications in namespace=$namespace")
        } catch (e: WebClientResponseException) {
            logger.error(e) { "Exception in scale, namespace=$namespace scaleCommand=$command status=${e.statusCode} body=${e.responseBodyAsString}" }
            throw RuntimeException(
                "Could not scale dc with name=${command.name} in namespace=$namespace causeStatusCode=${e.statusCode} causeMessage=${e.message}",
                e
            )
        }
    }

    @GetMapping("/pods/{namespace}")
    fun findPods(
        @PathVariable namespace: String,
        @RequestParam("applicationName", required = false) applicationName: String?
    ): ClerkResponse<PodItem> {
        validateUser(namespace)
        val podItems = podService.getPodItems(namespace, applicationName)

        val namePart = applicationName?.let { "name=$applicationName" } ?: ""
        val message1 = "Fetched count=${podItems.count()} pods for namespace=$namespace $namePart"
        logger.info(message1)
        return ClerkResponse(items = podItems, message = message1)
    }

    @DeleteMapping("/pods/{namespace}/{name}")
    fun deletePodAndScaleDown(
        @PathVariable namespace: String,
        @PathVariable name: String
    ): ClerkResponse<DeletePodAndScaleResult> {
        validateUser(namespace)
        try {
            deploymentConfigService.deletePodAndScaleDown(namespace, name)
            return ClerkResponse()
        } catch (e: WebClientResponseException) {
            logger.warn("DeletePodAndScaleDown failed response=${e.responseBodyAsString}")
            throw ClerkException(
                "Delete and/or scale operation failed, pod=$name in namespace=$namespace causeStatusCode=${e.statusCode} causeMessage=${e.message}",
                e
            )
        }
    }

    private fun validateUser(namespace: String) {
        val user = userProvider.getAuthenticatedUser()
        if (!user.username.startsWith("system:serviceaccount:$namespace:")) {
            throw BadCredentialsException("Only an application in the same namespace can use clerk.")
        }
    }
}
