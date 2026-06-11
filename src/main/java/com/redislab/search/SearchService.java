package com.redislab.search;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redislab.search.dto.RecentSearchesResponse;
import com.redislab.search.dto.SearchRequest;
import com.redislab.search.dto.SearchResponse;

/**
 * 사용자의 최근 검색어 내역을 Redis를 사용하여 관리하는 서비스 클래스이다.
 */
@Service
public class SearchService {

    private static final int MAX_RECENT_SEARCHES = 10;

    private final StringRedisTemplate redisTemplate;

    public SearchService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    /**
     * 새로운 검색어를 최근 검색어 목록에 추가한다.
     * 중복된 검색어는 제거되고 최상단(가장 최근)으로 이동하며, 최대 개수(10개)를 유지하도록 관리한다.
     * 
     * @param userId 사용자 ID
     * @param request request 검색 키워드를 포함한 요청 dto
     * @return 최신 검색어 목록을 포함한 응답 객체 
     */
    public SearchResponse addSearch(String userId, SearchRequest request){
        String normalizedUserId = normalize(userId);
        String keyword = normalizeKeyword(request.keyword());
        String key = buildRecentSearchesKey(normalizedUserId);

        // 기존 리스트에서 동일한 키워드가 있다면 모두 제거 (중복 방지)
        redisTemplate.opsForList().remove(key, 0, keyword);

        // 리스트의 맨 앞(왼쪽)에 새로운 검색어 추가
        redisTemplate.opsForList().leftPush(key, keyword);

        // 최근 검색어 개수를 최대 저장 개수(10개)로 제한 (0 ~ 9 인덱스까지만 남기고 잘라냄)
        redisTemplate.opsForList().trim(key, 0, MAX_RECENT_SEARCHES-1);

        return new SearchResponse(
            normalizedUserId,
            keyword,
            getRecentSearchesValue(key),
            "최근 검색어가 저장되었습니다."
        );
    }


    /**
     * 사용자의 최근 검색어 목록을 조회한다.
     * 
     * @param userId 사용자 ID
     * @return 최근 검색어 목록과 검색어 총 개수를 포함한 응답 dto
     */
    public RecentSearchesResponse getRecentSearches(String userId){
        String normalizedUserId = normalize(userId);
        String key = buildRecentSearchesKey(normalizedUserId);
        List<String> recentSearches = getRecentSearchesValue(key);

        return new RecentSearchesResponse(
            normalizedUserId,
            recentSearches,
            recentSearches.size()
        );
    }


    /**
     * 최근 검색어 목록에서 특정 키워드를 찾아 삭제한다.
     * 
     * @param userId 사용자 ID
     * @param keyword 삭제할 검색 키워드
     * @return 삭제 후 최신 검색어 목록을 포함한 dto
     */
    public SearchResponse deleteKeyword(String userId, String keyword){
        String normalizedUserId = normalize(userId);
        String normalizedKeyword = normalizeKeyword(keyword);
        String key = buildRecentSearchesKey(normalizedUserId);

        // 리스트에서 일치하는 키워드를 전체 삭제 (count가 0이면 일치하는 모든 요소 삭제)
        redisTemplate.opsForList().remove(key, 0, normalizedKeyword);

        return new SearchResponse(
            normalizedUserId,
            normalizedKeyword,
            getRecentSearchesValue(key),
            "최근 검색어에서 해당 keyword를 삭제했습니다."
        );
    }

    
    /**
     * 사용자의 전체 최근 검색어 내역을 삭제(초기화)한다.
     * 
     * @param userId 사용자 ID
     */
    public void cleanRecentSearches(String userId){
        String normalizedUserId = normalize(userId);
        String key = buildRecentSearchesKey(normalizedUserId);

        // 해당 사용자의 최근 검색어 Key 자체를 Redis에서 삭제
        redisTemplate.delete(key);
    }


    // ============================== 헬퍼 함수 ==============================

    /**
     * Redis에서 해당 Key에 저장된 최근 검색어 리스트를 조회한다.
     * range의 end 범위를 MAX_RECENT_SEARCHES로 지정하여 최대 개수만을 가져온다.
     * 
     * @param key Redis key
     * @return 검색어 문자열 리스트 (데이터가 없는 경우 빈 리스트 반환)
     */
    private List<String> getRecentSearchesValue(String key) {
        List<String> values = redisTemplate.opsForList().range(
                key,
                0,
                MAX_RECENT_SEARCHES - 1); // 0 ~ MAX_RECENT_SEARCHES-1 총 10개 조회
        
        if(values == null){
            return List.of();
        }

        return values;
    }

    /**
     * Redis에 저장할 최근 검색어 Key를 생성한다.
     * 형식 : user:{userId}:recent-searches
     */
    private String buildRecentSearchesKey(String userId){
        return "user:" + userId + ":recent-searches";
    }

    /**
     * 문자열의 앞뒤 공백 제거
     */
    private String normalize(String value){
        return value.trim();
    }

    /**
     * 검색어 키워드의 앞뒤 공백을 제거하고 소문자로 변환한다.
     * (대소문자 구분 없이 일관된 검색어 저장을 위함)
     */
    private String normalizeKeyword(String keyword){
        return keyword.trim().toLowerCase();
    }
}
