// AI 추천 캐싱 관리 클래스
class AIRecommendationCache {
    constructor() {
        this.cacheExpiry = 10 * 60 * 1000; // 10분
        this.maxCacheSize = 100;
        this.cacheKey = 'ai_popup_recommendations';
        this.timestampKey = 'ai_popup_recommendations_timestamp';
        this.memoryCache = new Map();
    }

    /**
     * 사용자별 캐시 키 생성
     */
    getUserSpecificCacheKey() {
        try {
            // apiService가 전역에서 사용 가능하므로 직접 호출
            const userId = apiService.getCurrentUserId();
            const isLoggedIn = apiService.isLoggedIn();

            if (isLoggedIn && userId) {
                return `${this.cacheKey}_user_${userId}`;
            } else {
                return `${this.cacheKey}_anonymous`;
            }
        } catch (error) {
            console.warn('사용자별 캐시 키 생성 실패:', error);
            return `${this.cacheKey}_default`;
        }
    }

    /**
     * 캐시 유효성 검사
     */
    isCacheValid() {
        const cacheKey = this.getUserSpecificCacheKey();
        const timestampKey = `${cacheKey}_timestamp`;

        // 메모리 캐시 확인
        if (this.memoryCache.has(cacheKey)) {
            const cacheData = this.memoryCache.get(cacheKey);
            if (Date.now() - cacheData.timestamp < this.cacheExpiry) {
                console.log('메모리 캐시 유효함');
                return true;
            } else {
                // 만료된 메모리 캐시 삭제
                this.memoryCache.delete(cacheKey);
                console.log('메모리 캐시 만료됨');
            }
        }

        // localStorage 캐시 확인
        try {
            const cachedData = localStorage.getItem(cacheKey);
            const cachedTimestamp = localStorage.getItem(timestampKey);

            if (cachedData && cachedTimestamp) {
                const timestamp = parseInt(cachedTimestamp);
                const now = Date.now();

                if (now - timestamp < this.cacheExpiry) {
                    // 메모리 캐시로 승격
                    this.memoryCache.set(cacheKey, {
                        data: JSON.parse(cachedData),
                        timestamp: timestamp
                    });
                    console.log('localStorage 캐시 유효함, 메모리로 승격');
                    return true;
                } else {
                    // 만료된 localStorage 캐시 삭제
                    localStorage.removeItem(cacheKey);
                    localStorage.removeItem(timestampKey);
                    console.log('localStorage 캐시 만료됨');
                }
            }
        } catch (error) {
            console.warn('캐시 유효성 검사 실패:', error);
            this.clearCache();
        }

        return false;
    }

    /**
     * 캐시 데이터 조회
     */
    getCachedData() {
        const cacheKey = this.getUserSpecificCacheKey();

        // 메모리 캐시에서 조회
        if (this.memoryCache.has(cacheKey)) {
            const cacheData = this.memoryCache.get(cacheKey);
            if (Date.now() - cacheData.timestamp < this.cacheExpiry) {
                console.log('메모리 캐시에서 데이터 반환');
                return cacheData.data;
            }
        }

        // localStorage에서 조회
        try {
            const cachedData = localStorage.getItem(cacheKey);
            if (cachedData) {
                const data = JSON.parse(cachedData);
                // 메모리 캐시로 승격
                this.memoryCache.set(cacheKey, {
                    data: data,
                    timestamp: Date.now()
                });
                console.log('localStorage에서 데이터 반환, 메모리로 승격');
                return data;
            }
        } catch (error) {
            console.warn('캐시 데이터 조회 실패:', error);
        }

        return null;
    }

    /**
     * 캐시 데이터 저장
     */
    setCacheData(data) {
        const cacheKey = this.getUserSpecificCacheKey();
        const timestampKey = `${cacheKey}_timestamp`;
        const timestamp = Date.now();

        try {
            // 입력 데이터 검증
            if (!Array.isArray(data) || data.length === 0) {
                console.warn('유효하지 않은 캐시 데이터:', data);
                return false;
            }

            // 메모리 캐시 크기 제한
            if (this.memoryCache.size >= this.maxCacheSize) {
                const oldestKey = this.memoryCache.keys().next().value;
                this.memoryCache.delete(oldestKey);
                console.log('오래된 메모리 캐시 삭제:', oldestKey);
            }

            // 메모리 캐시 저장
            this.memoryCache.set(cacheKey, {
                data: data,
                timestamp: timestamp
            });

            // localStorage 캐시 저장
            localStorage.setItem(cacheKey, JSON.stringify(data));
            localStorage.setItem(timestampKey, timestamp.toString());

            console.log(`AI 추천 캐시 저장 완료 - ${data.length}개 항목`);
            return true;

        } catch (error) {
            console.warn('캐시 저장 실패:', error);
            // localStorage 공간 부족 시 기존 캐시 정리 후 재시도
            if (error.name === 'QuotaExceededError') {
                this.clearOldCaches();
                try {
                    localStorage.setItem(cacheKey, JSON.stringify(data));
                    localStorage.setItem(timestampKey, timestamp.toString());
                    console.log('캐시 재시도 저장 성공');
                    return true;
                } catch (retryError) {
                    console.error('캐시 재시도 실패:', retryError);
                }
            }
            return false;
        }
    }

    /**
     * 캐시 무효화
     */
    invalidateCache() {
        const cacheKey = this.getUserSpecificCacheKey();
        const timestampKey = `${cacheKey}_timestamp`;

        // 메모리 캐시 삭제
        this.memoryCache.delete(cacheKey);

        // localStorage 캐시 삭제
        try {
            localStorage.removeItem(cacheKey);
            localStorage.removeItem(timestampKey);
            console.log('AI 추천 캐시 무효화 완료');
        } catch (error) {
            console.warn('캐시 무효화 실패:', error);
        }
    }

    /**
     * 전체 캐시 정리
     */
    clearCache() {
        // 메모리 캐시 전체 정리
        this.memoryCache.clear();

        // localStorage에서 AI 추천 관련 캐시만 정리
        try {
            const keysToRemove = [];
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith(this.cacheKey)) {
                    keysToRemove.push(key);
                }
            }

            keysToRemove.forEach(key => localStorage.removeItem(key));
            console.log(`${keysToRemove.length}개 캐시 항목 정리 완료`);

        } catch (error) {
            console.warn('캐시 정리 실패:', error);
        }
    }

    /**
     * 오래된 캐시들 정리
     */
    clearOldCaches() {
        const now = Date.now();
        const keysToRemove = [];

        try {
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith(this.cacheKey) && key.endsWith('_timestamp')) {
                    const timestamp = parseInt(localStorage.getItem(key) || '0');
                    if (now - timestamp > this.cacheExpiry) {
                        const dataKey = key.replace('_timestamp', '');
                        keysToRemove.push(key, dataKey);
                    }
                }
            }

            keysToRemove.forEach(key => localStorage.removeItem(key));
            if (keysToRemove.length > 0) {
                console.log(`${keysToRemove.length}개 만료된 캐시 정리 완료`);
            }

        } catch (error) {
            console.warn('오래된 캐시 정리 실패:', error);
        }
    }

    /**
     * 캐시 통계 조회
     */
    getCacheStats() {
        return {
            memoryCacheSize: this.memoryCache.size,
            localStorageCacheCount: this.getLocalStorageCacheCount(),
            isCurrentCacheValid: this.isCacheValid()
        };
    }

    /**
     * localStorage 캐시 개수 조회
     */
    getLocalStorageCacheCount() {
        let count = 0;
        try {
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith(this.cacheKey)) {
                    count++;
                }
            }
        } catch (error) {
            console.warn('localStorage 캐시 개수 조회 실패:', error);
        }
        return count;
    }
}

window.AIRecommendationCache = AIRecommendationCache;