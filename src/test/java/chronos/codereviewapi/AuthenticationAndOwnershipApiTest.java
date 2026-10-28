package chronos.codereviewapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.UserRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "JWT_SECRET=VGhpc0lzQVNlY3JldEtleUZvclRlc3RzMTIzNDU2Nzg5MA=="
})
@AutoConfigureMockMvc
@Transactional
class AuthenticationAndOwnershipApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registrationCreatesDeveloperAndRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        String registration = registrationBody(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void registrationRejectsInvalidEmailAndShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Developer","email":"invalid","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void developerCannotReadAnotherDevelopersReviewRequest() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail());
        String otherToken = registerAndLogin(uniqueEmail());

        String response = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Review private request",
                                  "description":"Only its owner may read this request.",
                                  "repositoryUrl":"https://github.com/example/project",
                                  "pullRequestUrl":"https://github.com/example/project/pull/1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number requestIdValue = com.jayway.jsonpath.JsonPath.read(response, "$.id");
        long requestId = requestIdValue.longValue();
        Number requesterId = com.jayway.jsonpath.JsonPath.read(response, "$.requester.id");
        Number tokenUserId = jwtDecoder.decode(ownerToken).getClaim("userId");
        assertEquals(requesterId.longValue(), tokenUserId.longValue());

        mockMvc.perform(get("/api/reviews/{id}", requestId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void assignedReviewerCanCommentDecideAndRecordStatusHistory() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail());
        Number requestId = createReviewRequest(ownerToken);
        String assignedReviewerToken = registerAndLoginAsReviewer();
        String otherReviewerToken = registerAndLoginAsReviewer();

        mockMvc.perform(patch("/api/reviews/{id}/claim", requestId.longValue())
                        .header("Authorization", "Bearer " + assignedReviewerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        mockMvc.perform(post("/api/reviews/{id}/comments", requestId.longValue())
                        .header("Authorization", "Bearer " + otherReviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Unauthorized comment\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reviews/{id}/comments", requestId.longValue())
                        .header("Authorization", "Bearer " + assignedReviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Add validation for expired JWTs.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Add validation for expired JWTs."));

        mockMvc.perform(post("/api/reviews/{id}/decision", requestId.longValue())
                        .header("Authorization", "Bearer " + assignedReviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"CHANGES_REQUESTED\",\"comment\":\"Please add the tests.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"));

        mockMvc.perform(get("/api/reviews/{id}/history", requestId.longValue())
                        .header("Authorization", "Bearer " + assignedReviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(3))
                .andExpect(jsonPath("$.history[0].newStatus").value("OPEN"))
                .andExpect(jsonPath("$.history[1].newStatus").value("IN_REVIEW"))
                .andExpect(jsonPath("$.history[2].newStatus").value("CHANGES_REQUESTED"));
    }

    private String registerAndLogin(String email) throws Exception {
        registerUser(email);
        return login(email);
    }

    private String registerAndLoginAsReviewer() throws Exception {
        String email = uniqueEmail();
        registerUser(email);
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.REVIEWER);
        userRepository.saveAndFlush(user);
        return login(email);
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(email)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.token");
    }

    private Number createReviewRequest(String ownerToken) throws Exception {
        String response = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Review workflow",
                                  "description":"Validate the end-to-end reviewer workflow.",
                                  "repositoryUrl":"https://github.com/example/project",
                                  "pullRequestUrl":"https://github.com/example/project/pull/2"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String registrationBody(String email) {
        return """
                {"name":"Developer","email":"%s","password":"Password123!"}
                """.formatted(email);
    }

    private String uniqueEmail() {
        return "developer-" + UUID.randomUUID() + "@example.com";
    }
}
