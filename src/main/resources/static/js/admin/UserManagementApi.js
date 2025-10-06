/**
 * 회원 관리 API 클래스
 */
class UserManagementApi {
    constructor() {
        this.baseURL = '/api/admin/users';
    }

    /**
     * 저장된 토큰 가져오기
     */
    getStoredToken() {
        return localStorage.getItem('authToken');
    }

    /**
     * 회원 검색
     * @param {Object} params 검색 파라미터
     * @returns {Promise<Object>} 검색 결과
     */
    async searchUsers(params) {
        try {
            const token = this.getStoredToken();
            const queryParams = new URLSearchParams();

            // 페이징 파라미터
            queryParams.append('page', params.page || 0);
            queryParams.append('size', params.size || 10);

            // 검색 조건
            if (params.keyword) {
                queryParams.append('searchType', params.searchType || 'name');
                queryParams.append('keyword', params.keyword);
            }
            if (params.role) {
                queryParams.append('role', params.role);
            }

            const response = await fetch(`${this.baseURL}/search?${queryParams}`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`검색 요청 실패: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('회원 검색 실패:', error);
            throw error;
        }
    }

    /**
     * 회원 상세 정보 조회
     * @param {number} userId 사용자 ID
     * @returns {Promise<Object>} 사용자 상세 정보
     */
    async getUserDetail(userId) {
        try {
            const token = this.getStoredToken();
            const response = await fetch(`${this.baseURL}/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`사용자 정보 조회 실패: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('사용자 상세 정보 조회 실패:', error);
            throw error;
        }
    }

    /**
     * 계정 전환 요청 개수 조회
     * @returns {Promise<number>} 요청 개수
     */
    async getUpgradeRequestCount() {
        try {
            const token = this.getStoredToken();
            const response = await fetch('/api/admin/users/upgrade-requests/count', {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`요청 개수 조회 실패: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('계정 전환 요청 개수 로드 실패:', error);
            return 0;
        }
    }

    /**
     * 전체 회원 수 조회
     * @returns {Promise<number>} 전체 회원 수
     */
    async getTotalUserCount() {
        try {
            const token = this.getStoredToken();
            const response = await fetch(`${this.baseURL}/count`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`전체 회원 수 조회 실패: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('전체 회원 수 조회 실패:', error);
            throw error;
        }
    }

    /**
     * 역할별 회원 수 조회
     * @returns {Promise<Object>} 역할별 통계
     */
    async getUserCountByRole() {
        try {
            const token = this.getStoredToken();
            const response = await fetch(`${this.baseURL}/count/by-role`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`역할별 통계 조회 실패: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('역할별 회원 수 조회 실패:', error);
            throw error;
        }
    }

    /**
     * 사용자 상태 변경
     * @param {string} userId 사용자 ID
     * @param {string} status 새로운 상태 ('ACTIVE' 또는 'INACTIVE')
     * @returns {Promise<Object>} 변경 결과
     */
    async updateUserStatus(userId, status) {
        try {
            const token = this.getStoredToken();
            console.log('사용자 상태 변경 API 호출 - userId:', userId, 'status:', status);

            const response = await fetch(`${this.baseURL}/${userId}/status`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    status: status
                })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                const errorMessage = errorData?.message || `상태 변경 실패: ${response.status} ${response.statusText}`;
                throw new Error(errorMessage);
            }

            const result = await response.json();
            console.log('사용자 상태 변경 API 응답:', result);

            return result;

        } catch (error) {
            console.error('사용자 상태 변경 API 실패:', error);
            throw error;
        }
    }
}