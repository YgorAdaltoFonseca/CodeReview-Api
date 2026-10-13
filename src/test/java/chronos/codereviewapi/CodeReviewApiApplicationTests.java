package chronos.codereviewapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "JWT_SECRET=VGhpc0lzQVNlY3JldEtleUZvclRlc3RzMTIzNDU2Nzg5MA=="
})
class CodeReviewApiApplicationTests {

    @Test
    void contextLoads() {
    }

}