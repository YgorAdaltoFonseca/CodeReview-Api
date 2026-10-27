package chronos.codereviewapi.dtos.requests;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewRequestRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsCompleteRequestWithHttpsUrls() {
        ReviewRequestRequest request = new ReviewRequestRequest(
                "Review authentication",
                "Check JWT validation and error handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingFieldsAndNonHttpsUrls() {
        ReviewRequestRequest request = new ReviewRequestRequest(
                " ",
                null,
                "http://github.com/ygor/project",
                "not a URL"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsFieldsThatExceedDatabaseColumnLimits() {
        ReviewRequestRequest request = new ReviewRequestRequest(
                "T".repeat(101),
                "D".repeat(2001),
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12"
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
