package chronos.codereviewapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "JWT_SECRET=VGhpc0lzQVNlY3JldEtleUZvclRlc3RzMTIzNDU2Nzg5MA=="
})
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticationIsRequiredOnlyForProtectedOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.security")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/reviews'].get.security[0].bearerAuth")
                        .exists())
                .andExpect(jsonPath("$.security").doesNotExist());
    }
}
