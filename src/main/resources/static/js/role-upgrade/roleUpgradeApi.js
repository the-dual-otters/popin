/**
 * 유저 승격 요청 API 서비스
 * 승격 요청 관련 HTTP 통신을 담당
 */
class RoleUpgradeApi {
    constructor() {
        this.baseURL = '/api';
        this.maxRetries = 3;
        this.retryDelay = 1000; // 1초
    }

    /**
     * 유저 승격 요청 API
     * @param {Object} requestData 승격 요청 데이터
     * @param {File} file 첨부 파일 (선택사항)
     * @returns {Promise<Object>} 승격 요청 응답 데이터
     */
    async createUpgradeRequest(requestData, file = null) {
        for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
            try {
                // FormData 생성
                const formData = new FormData();

                // 요청 데이터를 JSON으로 추가
                const requestBlob = new Blob([JSON.stringify(requestData)], {
                    type: 'application/json'
                });
                formData.append('request', requestBlob);

                // 파일이 있으면 추가
                if (file) {
                    formData.append('documents', file);
                }

                // 토큰 가져오기
                const token = this.getStoredToken();

                const headers = {};
                if (token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }

                const response = await fetch(`${this.baseURL}/role-upgrade/request`, {
                    method: 'POST',
                    headers: headers,
                    body: formData
                });

                const data = await response.json();

                if (!response.ok) {
                    const errorMessage = data.message || this.getHttpErrorMessage(response.status);
                    throw new Error(errorMessage);
                }

                return {
                    success: true,
                    message: data.message || '승격 요청이 성공적으로 제출되었습니다.',
                    requestId: data.requestId
                };

            } catch (error) {
                // 네트워크 에러인 경우 재시도
                if (error instanceof TypeError || error?.name === 'AbortError') {
                    if (attempt === this.maxRetries) {
                        throw new Error('네트워크 연결을 확인해주세요. 잠시 후 다시 시도해주세요.');
                    }
                    await this.delay(this.retryDelay * attempt);
                    continue;
                }
                // API 에러는 바로 throw
                throw error;
            }
        }
    }

    /**
     * 내 승격 요청 목록 조회
     * @returns {Promise<Array>} 승격 요청 목록
     */
    async getMyUpgradeRequests() {
        try {
            const token = this.getStoredToken();

            const headers = {
                'Content-Type': 'application/json'
            };

            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const response = await fetch(`${this.baseURL}/role-upgrade/my-requests`, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                throw new Error('승격 요청 목록 조회 중 오류가 발생했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('승격 요청 목록 조회 실패:', error);
            throw new Error('승격 요청 목록 조회 중 오류가 발생했습니다.');
        }
    }

    /**
     * 승격 요청 상세 조회
     * @param {number} requestId 요청 ID
     * @returns {Promise<Object>} 승격 요청 상세 정보
     */
    async getUpgradeRequestDetail(requestId) {
        try {
            const token = this.getStoredToken();

            const headers = {
                'Content-Type': 'application/json'
            };

            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const response = await fetch(`${this.baseURL}/user/role-upgrade/${requestId}`, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                throw new Error('승격 요청 상세 조회 중 오류가 발생했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('승격 요청 상세 조회 실패:', error);
            throw new Error('승격 요청 상세 조회 중 오류가 발생했습니다.');
        }
    }

    /**
     * HTTP 상태 코드에 따른 에러 메시지 반환
     * @param {number} status HTTP 상태 코드
     * @returns {string} 에러 메시지
     */
    getHttpErrorMessage(status) {
        const messages = {
            400: '입력 정보를 확인해주세요.',
            401: '로그인이 필요합니다.',
            403: '권한이 없습니다.',
            404: '요청한 리소스를 찾을 수 없습니다.',
            409: '이미 처리된 요청이거나 중복된 요청입니다.',
            422: '입력 데이터가 올바르지 않습니다.',
            429: '너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요.',
            500: '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
            502: '서버가 일시적으로 사용할 수 없습니다.',
            503: '서버가 일시적으로 사용할 수 없습니다.'
        };
        return messages[status] || `오류가 발생했습니다. (${status})`;
    }

    /**
     * 저장된 토큰 가져오기
     * @returns {string|null} 토큰
     */
    getStoredToken() {
        // api.js의 apiService와 호환성을 위해
        if (typeof apiService !== 'undefined' && apiService.getStoredToken) {
            return apiService.getStoredToken();
        }

        // 직접 토큰 조회
        return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    }

    /**
     * 지연 함수
     * @param {number} ms 밀리초
     * @returns {Promise}
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * 파일 크기 검증
     * @param {File} file 파일
     * @param {number} maxSizeMB 최대 크기 (MB)
     * @returns {boolean} 검증 결과
     */
    validateFileSize(file, maxSizeMB = 10) {
        if (!file) return true;

        const maxSizeBytes = maxSizeMB * 1024 * 1024;
        return file.size <= maxSizeBytes;
    }

    /**
     * 파일 타입 검증
     * @param {File} file 파일
     * @param {Array} allowedTypes 허용된 타입 목록
     * @returns {boolean} 검증 결과
     */
    validateFileType(file, allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf']) {
        if (!file) return true;

        return allowedTypes.includes(file.type);
    }

    /**
     * 업로드 진행률 콜백과 함께 파일 업로드
     * @param {Object} requestData 승격 요청 데이터
     * @param {File} file 첨부 파일
     * @param {Function} onProgress 진행률 콜백
     * @returns {Promise<Object>} 업로드 결과
     */
    async createUpgradeRequestWithProgress(requestData, file = null, onProgress = null) {
        return new Promise((resolve, reject) => {
            const formData = new FormData();

            // 요청 데이터를 JSON으로 추가
            const requestBlob = new Blob([JSON.stringify(requestData)], {
                type: 'application/json'
            });
            formData.append('request', requestBlob);

            // 파일이 있으면 추가
            if (file) {
                formData.append('documents', file);
            }

            const xhr = new XMLHttpRequest();

            // 진행률 이벤트
            if (onProgress) {
                xhr.upload.addEventListener('progress', (e) => {
                    if (e.lengthComputable) {
                        const percentComplete = (e.loaded / e.total) * 100;
                        onProgress(percentComplete);
                    }
                });
            }

            // 완료 이벤트
            xhr.addEventListener('load', () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const data = JSON.parse(xhr.responseText);
                        resolve({
                            success: true,
                            message: data.message || '승격 요청이 성공적으로 제출되었습니다.',
                            requestId: data.requestId
                        });
                    } catch (error) {
                        reject(new Error('응답 데이터 파싱 오류'));
                    }
                } else {
                    try {
                        const errorData = JSON.parse(xhr.responseText);
                        reject(new Error(errorData.message || this.getHttpErrorMessage(xhr.status)));
                    } catch (error) {
                        reject(new Error(this.getHttpErrorMessage(xhr.status)));
                    }
                }
            });

            // 에러 이벤트
            xhr.addEventListener('error', () => {
                reject(new Error('네트워크 오류가 발생했습니다.'));
            });

            // 타임아웃 이벤트
            xhr.addEventListener('timeout', () => {
                reject(new Error('요청 시간이 초과되었습니다.'));
            });

            // 요청 설정
            const token = this.getStoredToken();
            xhr.open('POST', `${this.baseURL}/user/role-upgrade/request`);

            if (token) {
                xhr.setRequestHeader('Authorization', `Bearer ${token}`);
            }

            xhr.timeout = 30000; // 30초 타임아웃

            // 요청 전송
            xhr.send(formData);
        });
    }
}