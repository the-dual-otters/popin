/**
 * 로그아웃 API 서비스
 * 로그아웃 관련 HTTP 통신과 토큰 정리를 담당
 */
class LogoutApi {
    constructor() {
        this.baseURL = '/api';
    }

    /**
     * 로그아웃 API 요청
     * @param {string} token 액세스 토큰 (선택적)
     * @returns {Promise<Object>} 로그아웃 응답 데이터
     */
    async logout(token = null) {
        try {
            // 토큰이 없으면 api.js에서 가져오기
            if (!token) {
                token = apiService.getStoredToken();
            }

            // 요청 데이터 준비
            const requestData = {};
            if (token) {
                requestData.accessToken = token;
            }

            // 헤더 설정
            const headers = {
                'Content-Type': 'application/json'
            };

            // 토큰이 있으면 Authorization 헤더 추가
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const response = await fetch(`${this.baseURL}/auth/logout`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(requestData)
            });

            const data = await response.json();

            if (!response.ok) {
                console.warn('로그아웃 API 응답 오류:', data);
                // 로그아웃은 실패해도 클라이언트에서 토큰을 정리하므로 계속 진행
            }

            return data;
        } catch (error) {
            console.warn('로그아웃 API 호출 실패 (계속 진행):', error);
            // 네트워크 오류 등이 발생해도 클라이언트에서 토큰 정리
            return { success: true, message: '로그아웃이 완료되었습니다.' };
        }
    }

    /**
     * 저장된 토큰 반환 (api.js 활용)
     * @returns {string|null} 액세스 토큰
     */
    getStoredToken() {
        return apiService.getStoredToken();
    }

    /**
     * 모든 인증 관련 데이터 제거 (api.js 활용)
     */
    clearAllAuthData() {
        // api.js의 토큰 제거 메서드 사용
        apiService.removeToken();

        // 사용자 정보 제거
        const authKeys = [
            'userName',
            'userEmail',
            'userId',
            'userRole',
            'loginTime'
        ];

        // localStorage에서 제거
        authKeys.forEach(key => {
            try {
                localStorage.removeItem(key);
            } catch (e) {
                console.warn(`localStorage에서 ${key} 제거 실패:`, e);
            }
        });

        // sessionStorage에서 제거
        authKeys.forEach(key => {
            try {
                sessionStorage.removeItem(key);
            } catch (e) {
                console.warn(`sessionStorage에서 ${key} 제거 실패:`, e);
            }
        });

        // 쿠키 제거 (jwtToken)
        this.clearAuthCookies();
    }

    /**
     * 인증 관련 쿠키 제거
     */
    clearAuthCookies() {
        const authCookies = ['jwtToken', 'JSESSIONID'];

        authCookies.forEach(cookieName => {
            // 현재 도메인에서 제거
            document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;

            // 루트 도메인에서도 제거 시도
            const domain = window.location.hostname;
            if (domain.includes('.')) {
                const rootDomain = '.' + domain.split('.').slice(-2).join('.');
                document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=${rootDomain}`;
            }
        });
    }

    /**
     * 로그인 상태 확인 (api.js 토큰 기반)
     * @returns {boolean} 로그인 여부
     */
    isLoggedIn() {
        const token = this.getStoredToken();
        if (!token) return false;

        // JWT 토큰 만료 체크
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return Date.now() < payload.exp * 1000;
        } catch (error) {
            return false;
        }
    }

    /**
     * 완전한 로그아웃 처리 (API 호출 + 데이터 정리)
     * @returns {Promise<Object>} 로그아웃 결과
     */
    async performFullLogout() {
        const token = this.getStoredToken();

        try {
            // 1. 서버에 로그아웃 요청
            const result = await this.logout(token);

            // 2. 클라이언트 데이터 정리 (API 성공/실패 관계없이 실행)
            this.clearAllAuthData();

            return {
                success: true,
                message: result.message || '로그아웃이 완료되었습니다.',
                serverResponse: result
            };
        } catch (error) {
            // 오류가 발생해도 클라이언트 데이터는 정리
            this.clearAllAuthData();

            return {
                success: true, // 클라이언트 관점에서는 성공
                message: '로그아웃이 완료되었습니다.',
                error: error.message
            };
        }
    }
}