package io.github.kocoding.taskmanager.common.query;

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
            // 2. 許可されたものは JpaSort.unsafe(向き, 許可リストの式) に変換
            safeSort = safeSort.and(JpaSort.unsafe(order.getDirection(), allowList.get(order.getProperty())));
        }

        // 3. sortの指定がなければ defaultSort を defaultDirectionの向き
        if (safeSort.isUnsorted()) {
            safeSort = safeSort.and(JpaSort.unsafe(defaultDirection, defaultSort));
        }

        // 4. 最後に tieBreaker の昇順を必ず足す
        safeSort = safeSort.and(JpaSort.unsafe(Sort.Direction.ASC, tieBreaker));

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

}
