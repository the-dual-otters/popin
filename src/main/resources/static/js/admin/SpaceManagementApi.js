/**
 * 장소 관리 API 클래스
 */
class SpaceManagementApi {
    constructor() {
        this.baseURL = '/api/admin/spaces';
    }

    /**
     * 토큰 가져오기
     */
    getToken() {
        return localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
    }

    /**
     * 장소 통계 조회
     */
    async getSpaceStats() {
        try {
            const response = await fetch(`${this.baseURL}/stats`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('장소 통계 조회 실패:', error);
            throw error;
        }
    }

    /**
     * 장소 목록 조회 (관리자용) - isPublic → isHidden 파라미터 변경
     */
    async getSpaces(params = {}) {
        try {
            const queryParams = new URLSearchParams();

            // 페이징
            if (params.page !== undefined) queryParams.append('page', params.page);
            if (params.size !== undefined) queryParams.append('size', params.size);

            // 필터링 - isPublic → isHidden으로 변경
            if (params.owner) queryParams.append('owner', params.owner);
            if (params.title) queryParams.append('title', params.title);
            if (params.isHidden !== undefined) queryParams.append('isHidden', params.isHidden);

            console.log('🌐 API 요청 URL:', `${this.baseURL}?${queryParams.toString()}`);

            const response = await fetch(`${this.baseURL}?${queryParams.toString()}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('장소 목록 조회 실패:', error);
            throw error;
        }
    }

    /**
     * 장소 상세 조회 (관리자용)
     */
    async getSpaceDetail(spaceId) {
        try {
            const response = await fetch(`${this.baseURL}/${spaceId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('장소 상세 조회 실패:', error);
            throw error;
        }
    }

    /**
     * 장소 상태 토글 (관리자용) - 새로 추가된 메소드
     */
    async toggleSpaceVisibility(spaceId) {
        try {
            const response = await fetch(`${this.baseURL}/${spaceId}/toggle-visibility`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('장소 상태 토글 실패:', error);
            throw error;
        }
    }

    /**
     * 에러 처리
     */
    handleError(error) {
        console.error('API 에러:', error);

        if (error.message.includes('401')) {
            alert('인증이 만료되었습니다. 다시 로그인해주세요.');
            window.location.href = '/templates/pages/auth/login.html';
        } else if (error.message.includes('403')) {
            alert('권한이 없습니다.');
        } else if (error.message.includes('404')) {
            alert('요청한 리소스를 찾을 수 없습니다.');
        } else {
            alert('오류가 발생했습니다: ' + error.message);
        }
    }
}