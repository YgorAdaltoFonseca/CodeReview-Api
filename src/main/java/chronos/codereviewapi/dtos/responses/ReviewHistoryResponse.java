package chronos.codereviewapi.dtos.responses;

import java.util.List;

public record ReviewHistoryResponse(
        Long reviewRequestId,
        List<ReviewStatusHistoryItemResponse> history
) {
}
