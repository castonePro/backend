package com.capstone.travelbusan.domain.planner.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 단위 슬라이딩 윈도우 요청 제한.
 *
 * <p>플래너 생성·대화 엔드포인트가 인증 없이 열려 있어서, 세션당 상한만으로는
 * "세션을 계속 새로 만드는" 남용을 막을 수 없다. 여기가 그 구멍을 막는다.
 *
 * <p>외부 라이브러리(Bucket4j 등) 없이 메모리로만 돌린다. 인스턴스가 여러 대로
 * 늘어나면 인스턴스별 카운트가 되므로, 그때는 Redis 기반으로 바꿔야 한다.
 */
@Slf4j
@Component
public class SessionRateLimiter {

    private static final int MAX_REQUESTS = 30;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    /** 메모리가 무한정 늘지 않도록 추적 대상 수를 제한한다. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** @return 허용되면 true */
    public boolean tryAcquire(String clientKey) {
        String key = (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey;
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW.toMillis();

        if (hits.size() > MAX_TRACKED_CLIENTS) {
            prune(cutoff);
        }

        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                log.warn("요청 한도 초과: key={}, {}분 내 {}회", key, WINDOW.toMinutes(), timestamps.size());
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    public int maxRequestsPerWindow() {
        return MAX_REQUESTS;
    }

    public long windowMinutes() {
        return WINDOW.toMinutes();
    }

    private void prune(long cutoff) {
        hits.entrySet().removeIf(entry -> {
            Deque<Long> q = entry.getValue();
            synchronized (q) {
                while (!q.isEmpty() && q.peekFirst() < cutoff) {
                    q.pollFirst();
                }
                return q.isEmpty();
            }
        });
    }
}
