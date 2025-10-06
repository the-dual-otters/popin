/**
 * 로그아웃 컨트롤러
 * 로그아웃 프로세스를 관리하고 UI 피드백 제공
 */
class LogoutController {
    constructor() {
        this.logoutApi = new LogoutApi();
        this.isLoggingOut = false; // 중복 요청 방지
    }

    /**
     * 로그아웃 실행
     * @param {Object} options 로그아웃 옵션
     * @param {boolean} options.showConfirm 확인 대화상자 표시 여부
     * @param {Function} options.onSuccess 성공 콜백
     * @param {Function} options.onError 오류 콜백
     * @param {string} options.redirectUrl 리다이렉트 URL
     * @returns {Promise<boolean>} 로그아웃 성공 여부
     */
    async performLogout(options = {}) {
        const {
            showConfirm = true,
            onSuccess = null,
            onError = null,
            redirectUrl = '/auth/login?logout=true'
        } = options;

        // 중복 실행 방지
        if (this.isLoggingOut) {
            console.warn('로그아웃이 이미 진행 중입니다.');
            return false;
        }

        // 로그인 상태 확인
        if (!this.logoutApi.isLoggedIn()) {
            console.info('이미 로그아웃 상태입니다.');
            if (redirectUrl) {
                window.location.href = redirectUrl;
            }
            return true;
        }

        // 확인 대화상자
        if (showConfirm) {
            const confirmed = confirm('로그아웃 하시겠습니까?');
            if (!confirmed) {
                return false;
            }
        }

        this.isLoggingOut = true;

        try {
            // 로딩 UI 표시
            this.showLogoutLoading();

            // 로그아웃 실행
            const result = await this.logoutApi.performFullLogout();

            console.info('로그아웃 완료:', result);

            // 성공 콜백 실행
            if (onSuccess && typeof onSuccess === 'function') {
                try {
                    await onSuccess(result);
                } catch (callbackError) {
                    console.warn('성공 콜백 실행 중 오류:', callbackError);
                }
            }

            // 성공 메시지 표시 (짧게)
            this.showLogoutMessage('로그아웃되었습니다.', 'success');

            // 페이지 리다이렉트
            if (redirectUrl) {
                setTimeout(() => {
                    window.location.href = redirectUrl;
                }, 1000);
            }

            return true;

        } catch (error) {
            console.error('로그아웃 처리 중 오류:', error);

            // 오류 콜백 실행
            if (onError && typeof onError === 'function') {
                try {
                    onError(error);
                } catch (callbackError) {
                    console.warn('오류 콜백 실행 중 오류:', callbackError);
                }
            }

            // 오류 메시지 표시
            this.showLogoutMessage('로그아웃 중 오류가 발생했지만 로그아웃되었습니다.', 'warning');

            // 오류가 발생해도 로그인 페이지로 이동
            if (redirectUrl) {
                setTimeout(() => {
                    window.location.href = redirectUrl;
                }, 2000);
            }

            return false;

        } finally {
            this.isLoggingOut = false;
            this.hideLogoutLoading();
        }
    }

    /**
     * 빠른 로그아웃 (확인 없이)
     * @param {string} redirectUrl 리다이렉트 URL
     * @returns {Promise<boolean>} 로그아웃 성공 여부
     */
    async quickLogout(redirectUrl = '/auth/login?logout=true') {
        return await this.performLogout({
            showConfirm: false,
            redirectUrl: redirectUrl
        });
    }

    /**
     * 조용한 로그아웃 (UI 피드백 없이)
     * @returns {Promise<boolean>} 로그아웃 성공 여부
     */
    async silentLogout() {
        if (this.isLoggingOut) {
            return false;
        }

        this.isLoggingOut = true;

        try {
            const result = await this.logoutApi.performFullLogout();
            console.info('조용한 로그아웃 완료:', result);
            return true;
        } catch (error) {
            console.warn('조용한 로그아웃 중 오류:', error);
            return false;
        } finally {
            this.isLoggingOut = false;
        }
    }

    /**
     * 로그아웃 로딩 UI 표시
     */
    showLogoutLoading() {
        // 기존 로딩 요소 제거
        this.hideLogoutLoading();

        // 로딩 오버레이 생성
        const overlay = document.createElement('div');
        overlay.id = 'logout-loading-overlay';
        overlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        `;

        const loadingContent = document.createElement('div');
        loadingContent.style.cssText = `
            background: white;
            padding: 30px 40px;
            border-radius: 12px;
            text-align: center;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
        `;

        const spinner = document.createElement('div');
        spinner.style.cssText = `
            width: 24px;
            height: 24px;
            border: 3px solid #f3f4f6;
            border-radius: 50%;
            border-top-color: #4B5AE4;
            animation: spin 1s ease-in-out infinite;
            margin: 0 auto 15px;
        `;

        const message = document.createElement('div');
        message.textContent = '로그아웃 중...';
        message.style.cssText = `
            color: #374151;
            font-size: 16px;
            font-weight: 500;
        `;

        // 스피너 애니메이션 추가
        if (!document.getElementById('logout-spinner-style')) {
            const style = document.createElement('style');
            style.id = 'logout-spinner-style';
            style.textContent = `
                @keyframes spin {
                    to { transform: rotate(360deg); }
                }
            `;
            document.head.appendChild(style);
        }

        loadingContent.appendChild(spinner);
        loadingContent.appendChild(message);
        overlay.appendChild(loadingContent);
        document.body.appendChild(overlay);
    }

    /**
     * 로그아웃 로딩 UI 숨기기
     */
    hideLogoutLoading() {
        const overlay = document.getElementById('logout-loading-overlay');
        if (overlay) {
            overlay.remove();
        }
    }

    /**
     * 로그아웃 메시지 표시
     * @param {string} message 메시지 내용
     * @param {string} type 메시지 타입 (success, error, warning)
     */
    showLogoutMessage(message, type = 'success') {
        // 기존 메시지 제거
        const existingMessage = document.getElementById('logout-message');
        if (existingMessage) {
            existingMessage.remove();
        }

        const messageDiv = document.createElement('div');
        messageDiv.id = 'logout-message';

        let backgroundColor, textColor, borderColor;
        switch (type) {
            case 'success':
                backgroundColor = '#d1fae5';
                textColor = '#065f46';
                borderColor = '#a7f3d0';
                break;
            case 'error':
                backgroundColor = '#fee2e2';
                textColor = '#991b1b';
                borderColor = '#fca5a5';
                break;
            case 'warning':
                backgroundColor = '#fef3c7';
                textColor = '#92400e';
                borderColor = '#fde68a';
                break;
            default:
                backgroundColor = '#dbeafe';
                textColor = '#1e40af';
                borderColor = '#93c5fd';
        }

        messageDiv.style.cssText = `
            position: fixed;
            top: 20px;
            left: 50%;
            transform: translateX(-50%);
            background: ${backgroundColor};
            color: ${textColor};
            border: 1px solid ${borderColor};
            padding: 12px 24px;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 500;
            z-index: 10000;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        `;

        messageDiv.textContent = message;
        document.body.appendChild(messageDiv);

        // 3초 후 자동 제거
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.remove();
            }
        }, 3000);
    }
    async handleSubmit(e) {
        e.preventDefault();

        console.log('회원가입 폼 제출 시작');

        // 클라이언트 사이드 검증
        if (!this.ui.validateForm()) {
            console.log('폼 검증 실패');
            this.ui.showAlert('입력 정보를 확인해주세요.', 'error');
            return;
        }

        // 중복확인 상태 체크
        if (!this.validateDuplicateChecks()) {
            console.log('중복확인 검증 실패');
            return;
        }

        const formData = this.ui.getFormData();
        const selectedTags = this.ui.getSelectedTags();

        console.log('선택된 관심사 태그:', selectedTags);

        // 회원가입 데이터 준비
        const signupData = this.signupApi.prepareSignupData(formData, selectedTags);

        console.log('회원가입 데이터:', {
            ...signupData,
            password: '***', // 보안상 로그에서 제외
            passwordConfirm: '***'
        });

        // 최종 검증
        if (!this.validateSignupData(signupData)) {
            console.log('최종 검증 실패');
            return;
        }

        this.ui.toggleLoading(true);

        try {
            const response = await this.signupApi.signup(signupData);

            console.log('회원가입 응답:', response);

            if (response.success) {
                // 성공 메시지 표시
                console.log('회원가입 성공, 알림 표시 시도');
                this.ui.showAlert('회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.', 'success');

                // 폼 리셋
                this.ui.resetForm();

                // 로그인 페이지로 리다이렉트
                setTimeout(() => {
                    window.location.href = '/auth/login?signup=success';
                }, 2000);
            } else {
                throw new Error(response.message || '회원가입에 실패했습니다.');
            }
        } catch (error) {
            console.error('회원가입 실패:', error);
            this.handleSignupError(error);
        } finally {
            this.ui.toggleLoading(false);
        }
    }

    /**
     * 페이지의 로그아웃 버튼들에 이벤트 리스너 추가
     * @param {string} selector 로그아웃 버튼 선택자
     */
    bindLogoutButtons(selector = '.logout-btn, [data-action="logout"]') {
        document.addEventListener('click', async (event) => {
            const target = event.target.closest(selector);
            if (!target) return;

            event.preventDefault();

            // 버튼의 data 속성에서 옵션 읽기
            const showConfirm = target.dataset.confirm !== 'false';
            const redirectUrl = target.dataset.redirect || '/auth/login?logout=true';

            await this.performLogout({
                showConfirm,
                redirectUrl
            });
        });
    }

    /**
     * 자동 로그아웃 (토큰 만료 등)
     * @param {string} reason 로그아웃 사유
     * @param {string} redirectUrl 리다이렉트 URL
     */
    async autoLogout(reason = '세션이 만료되었습니다.', redirectUrl = '/auth/login?expired=true') {
        console.info('자동 로그아웃:', reason);

        this.showLogoutMessage(reason, 'warning');

        // 조용한 로그아웃 수행
        await this.silentLogout();

        // 페이지 이동
        setTimeout(() => {
            window.location.href = redirectUrl;
        }, 2000);
    }
}

// 전역 로그아웃 컨트롤러 인스턴스
const logoutController = new LogoutController();