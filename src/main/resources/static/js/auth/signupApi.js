/**
 * 회원가입 API 서비스
 * 회원가입 관련 HTTP 통신과 중복 확인을 담당
 */
class SignupApi {
    constructor() {
        this.baseURL = '/api';
        this.maxRetries = 3;
        this.retryDelay = 1000; // 1초
    }

    /**
     * 회원가입 API 요청
     * @param {Object} signupData 회원가입 데이터
     * @returns {Promise<Object>} 회원가입 응답 데이터
     */
    async signup(signupData) {
        for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
            try {
                const response = await fetch(`${this.baseURL}/auth/signup`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(signupData)
                });

                const data = await response.json();

                if (!response.ok) {
                    const errorMessage = data.message || this.getHttpErrorMessage(response.status);
                    throw new Error(errorMessage);
                }

                return data;
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
     * 이메일 중복 확인 API
     * @param {string} email 확인할 이메일
     * @returns {Promise<Object>} { available: boolean, exists: boolean }
     */
    async checkEmailDuplicate(email) {
        try {
            const response = await fetch(`${this.baseURL}/auth/check-email?email=${encodeURIComponent(email)}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('이메일 중복 확인 중 오류가 발생했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('이메일 중복 확인 실패:', error);
            throw new Error('이메일 중복 확인 중 오류가 발생했습니다.');
        }
    }

    /**
     * 닉네임 중복 확인 API
     * @param {string} nickname 확인할 닉네임
     * @returns {Promise<Object>} { available: boolean, exists: boolean }
     */
    async checkNicknameDuplicate(nickname) {
        try {
            const response = await fetch(`${this.baseURL}/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('닉네임 중복 확인 중 오류가 발생했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('닉네임 중복 확인 실패:', error);
            throw new Error('닉네임 중복 확인 중 오류가 발생했습니다.');
        }
    }

    /**
     * 전체 카테고리 목록 조회 API
     * @returns {Promise<Array>} 카테고리 목록
     */
    async getAllCategories() {
        try {
            const response = await fetch(`${this.baseURL}/auth/categories`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('카테고리 목록을 불러오는데 실패했습니다.');
            }

            const data = await response.json();

            if (data.success && data.data) {
                return data.data; // CategoryResponseDto 배열
            } else {
                throw new Error('카테고리 데이터 형식이 올바르지 않습니다.');
            }
        } catch (error) {
            console.error('카테고리 목록 조회 실패:', error);
            throw new Error('카테고리 목록을 불러오는데 실패했습니다.');
        }
    }

    /**
     * 이메일 유효성 검증
     * @param {string} email 이메일
     * @returns {Promise<Object>} 검증 결과
     */
    async validateEmail(email) {
        try {
            const result = await this.checkEmailDuplicate(email);

            if (result.available) {
                return {
                    isValid: true,
                    message: '사용 가능한 이메일입니다.'
                };
            } else {
                return {
                    isValid: false,
                    message: '이미 사용 중인 이메일입니다.'
                };
            }
        } catch (error) {
            throw new Error(error.message || '이메일 검증 중 오류가 발생했습니다.');
        }
    }

    /**
     * 닉네임 유효성 검증
     * @param {string} nickname 닉네임
     * @returns {Promise<Object>} 검증 결과
     */
    async validateNickname(nickname) {
        try {
            const result = await this.checkNicknameDuplicate(nickname);

            if (result.available) {
                return {
                    isValid: true,
                    message: '사용 가능한 닉네임입니다.'
                };
            } else {
                return {
                    isValid: false,
                    message: '이미 사용 중인 닉네임입니다.'
                };
            }
        } catch (error) {
            throw new Error(error.message || '닉네임 검증 중 오류가 발생했습니다.');
        }
    }

    /**
     * 회원가입 데이터 준비
     * @param {FormData} formData 폼 데이터
     * @param {Array} selectedTags 선택된 관심사 태그
     * @returns {Object} 회원가입 요청 데이터
     */
    prepareSignupData(formData, selectedTags = []) {
        return {
            name: formData.get('name').trim(),
            nickname: formData.get('nickname').trim(),
            email: formData.get('email').trim(),
            password: formData.get('password'),
            passwordConfirm: formData.get('passwordConfirm'),
            phone: formData.get('phone').trim(),
            interests: selectedTags // 관심사 배열 추가
        };
    }

    /**
     * HTTP 상태 코드에 따른 에러 메시지 반환
     * @param {number} status HTTP 상태 코드
     * @returns {string} 에러 메시지
     */
    getHttpErrorMessage(status) {
        switch (status) {
            case 400:
                return '잘못된 요청입니다. 입력 정보를 확인해주세요.';
            case 409:
                return '이미 존재하는 정보입니다.';
            case 422:
                return '입력 정보가 올바르지 않습니다.';
            case 500:
                return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
            default:
                return '회원가입 중 오류가 발생했습니다.';
        }
    }

    /**
     * 지연 함수
     * @param {number} ms 지연 시간 (밀리초)
     * @returns {Promise} 지연 Promise
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}