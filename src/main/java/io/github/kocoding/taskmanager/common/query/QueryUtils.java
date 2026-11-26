package io.github.kocoding.taskmanager.common.query;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

import io.github.kocoding.taskmanager.common.exception.BadRequestException;

public final class QueryUtils {

    private QueryUtils() {
    }

    public static Pageable toSafePageable(Pageable pageable, Map<String, String> allowList, String defaultSort,
            Sort.Direction defaultDirection, String tieBreaker) {
        Sort safeSort = Sort.unsorted();
        Sort sort = pageable.getSort();

        // 1. pageable.getSort() の各 Order を、許可リストと照らし合わせる
        for (Sort.Order order : sort) {
            if (!allowList.containsKey(order.getProperty())) {
                // なければ BadRequestException
                throw new BadRequestException("sortに指定できない項目です:" + order.getProperty());
            }
            // 2. 許可されたものは、nullを最後にした並び(nullsLast)に変換
            safeSort = safeSort.and(nullsLast(order.getDirection(), allowList.get(order.getProperty())));
        }

        // 3. sortの指定がなければ、defaultSort を defaultDirectionの向きで並べる
        if (safeSort.isUnsorted()) {
            safeSort = safeSort.and(nullsLast(defaultDirection, defaultSort));
        }

        // 4. 最後に tieBreaker の昇順を必ず足す
        safeSort = safeSort.and(nullsLast(Sort.Direction.ASC, tieBreaker));

        // 5. PageRequest.of(ページ番号, size, sort) で返す
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }

    public static String toLikePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "%";
        }
        keyword = keyword.replace("\\", "\\\\");
        keyword = keyword.replace("%", "\\%");
        keyword = keyword.replace("_", "\\_");

        return "%" + keyword + "%";
    }

    private static Sort nullsLast(Sort.Direction direction, String expression) {
        List<Sort.Order> orders = JpaSort.unsafe(direction, expression).stream()
                .map(order -> order.with(Sort.NullHandling.NULLS_LAST))
                .toList();
        return Sort.by(orders);
    }

}
