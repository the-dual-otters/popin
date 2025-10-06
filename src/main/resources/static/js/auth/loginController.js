/**
 * 로그인 페이지 메인 컨트롤러
 * LoginApi, UI, Validator를 조합하여 로그인 플로우를 관리
 */
class LoginController {
    constructor() {
        this.loginApi = new LoginApi();
        this.ui = new LoginUI();

        this.init();
    }

    /**
     * 컨트롤러 초기화
     */
    init() {
        this.checkUrlParams();
        this.checkExistingToken();
        this.setupFormSubmission();
    }

    /**
     * URL 파라미터 확인 (에러, 로그아웃 메시지 등)
     */
    checkUrlParams() {
        const urlParams = new URLSearchParams(window.location.search);

        if (urlParams.get('error')) {
            this.ui.showAlert('아이디 또는 비밀번호를 확인해 주세요', 'error');
        }

        if (urlParams.get('logout')) {
            this.ui.showAlert('로그아웃되었습니다.', 'success');
        }

        if (urlParams.get('expired')) {
            this.ui.showAlert('세션이 만료되었습니다. 다시 로그인해주세요.', 'error');
        }

        if (urlParams.get('message')) {
            this.ui.showAlert(decodeURIComponent(urlParams.get('message')), 'info');
        }

        // URL 파라미터 정리
        if (urlParams.toString()) {
            const redirectParam = urlParams.get('redirect');

            if (redirectParam) {
                // redirect 파라미터만 남기고 나머지 제거
                const cleanParams = new URLSearchParams();
                cleanParams.set('redirect', redirectParam);
                window.history.replaceState(
                    {},
                    document.title,
                    `${window.location.pathname}?${cleanParams.toString()}`
                );
            } else {
                // redirect가 없으면 모든 파라미터 제거
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        }
    }

    /**
     * 기존 토큰 확인 및 자동 리다이렉트
     */
    async checkExistingToken() {
        const token = this.loginApi.getStoredToken();

        if (!window.location.pathname.startsWith('/auth/login')) {
            return; // 로그인 페이지가 아닐 때는 실행 안 함
        }

        if (token && !this.loginApi.isTokenExpired()) {
            // redirect 파라미터 확인
            const urlParams = new URLSearchParams(window.location.search);
            const redirectUrl = urlParams.get('redirect');

            if (redirectUrl) {
                // redirect 파라미터가 있으면 자동으로 그 페이지로 이동
                console.log('이미 로그인됨, 원래 페이지로 이동:', redirectUrl);
                window.location.href = decodeURIComponent(redirectUrl);
            } else {
                // redirect 파라미터가 없으면 확인창 띄우기
                const shouldRedirect = confirm('이미 로그인되어 있습니다. 메인 페이지로 이동하시겠습니까?');
                if (shouldRedirect) {
                    window.location.href = '/';
                }
            }
        } else if (token) {
            // 만료된 토큰 제거
            this.loginApi.removeToken();
            this.ui.showAlert('로그인이 만료되었습니다. 다시 로그인해주세요.', 'error');
        }
    }

    /**
     * 폼 제출 이벤트 설정
     */
    setupFormSubmission() {
        this.ui.elements.form.addEventListener('submit', (e) => {
            this.handleSubmit(e);
        });
    }

    /**
     * 폼 제출 처리
     * @param {Event} e 폼 제출 이벤트
     */
    async handleSubmit(e) {
        e.preventDefault();

        if (!this.ui.validateForm()) {
            return;
        }

        const formData = this.ui.getFormData();
        const email = formData.get('email').trim();
        const password = formData.get('password');

        this.ui.toggleLoading(true);

        try {
            const response = await this.loginApi.login(email, password);

            if (response.accessToken) {
                // 토큰 저장
                this.loginApi.storeToken(response.accessToken, response);

                // 성공 메시지 표시
                this.ui.showAlert('로그인 성공!', 'success');

                // 역할에 따른 리디렉션 ← 이 부분만 수정
                setTimeout(() => {
                    if (response.role === 'ADMIN') {
                        window.location.href = '/templates/admin/admin-inquiry-main.html';
                    } else {
                        window.location.href = '/';
                    }
                }, 1500);
            } else {
                throw new Error('로그인 응답이 올바르지 않습니다.');
            }
        } catch (error) {
            console.error('로그인 실패:', error);
            this.ui.showAlert(
                error.message || '로그인에 실패했습니다. 다시 시도해주세요.',
                'error'
            );
        } finally {
            this.ui.toggleLoading(false);
        }
    }
}