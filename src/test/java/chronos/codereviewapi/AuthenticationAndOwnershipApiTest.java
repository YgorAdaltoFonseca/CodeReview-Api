package chronos.codereviewapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "JWT_SECRET=VGhpc0lzQVNlY3JldEtleUZvclRlc3RzMTIzNDU2Nzg5MA=="
})
@AutoConfigureMockMvc
@Transactional
class AuthenticationAndOwnershipApiTest {

    @Autowired
    private MockMvc mockMvc;

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

        mockMvc.perform(get("/api/reviews/{id}", requestId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(email)))
                .andExpect(status().isCreated());

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

    private String registrationBody(String email) {
        return """
                {"name":"Developer","email":"%s","password":"Password123!"}
                """.formatted(email);
    }

    private String uniqueEmail() {
        return "developer-" + UUID.randomUUID() + "@example.com";
    }
}
