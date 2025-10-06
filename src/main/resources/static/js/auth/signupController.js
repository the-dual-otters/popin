/**
 * 회원가입 페이지 메인 컨트롤러
 */
class SignupController {
    constructor() {
        this.signupApi = new SignupApi();
        this.ui = new SignupUI();
        this.init();
    }

    /**
     * 컨트롤러 초기화
     */
    init() {
        this.setupFormSubmission();
        this.setupInitialState();
    }

    /**
     * 초기 상태 설정
     */
    async setupInitialState() {
        try {
            await this.ui.loadCategories();
            this.ui.updateSubmitButton();
        } catch (error) {
            console.error('초기화 실패:', error);
            this.ui.showAlert('페이지 초기화 중 오류가 발생했습니다.', 'error');
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
     */
    async handleSubmit(e) {
        e.preventDefault();

        // 기본 검증
        if (!this.ui.validateForm()) {
            this.ui.showAlert('입력 정보를 확인해주세요.', 'error');
            return;
        }

        // 중복확인 상태 체크
        if (!this.validateDuplicateChecks()) {
            return;
        }

        const formData = this.ui.getFormData();
        const selectedTags = this.ui.getSelectedTags();
        const signupData = this.signupApi.prepareSignupData(formData, selectedTags);

        this.ui.toggleLoading(true);

        try {
            const response = await this.signupApi.signup(signupData);

            if (response.success) {
                this.ui.showAlert('회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.', 'success');
                this.ui.resetForm();

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
     * 중복확인 상태 검증
     */
    validateDuplicateChecks() {
        const formData = this.ui.getFormData();
        const email = formData.get('email').trim();
        const nickname = formData.get('nickname').trim();

        if (email && !this.ui.validationStates.email) {
            this.ui.showAlert('이메일 중복확인을 완료해주세요.', 'error');
            this.ui.elements.emailInput.focus();
            return false;
        }

        if (nickname && !this.ui.validationStates.nickname) {
            this.ui.showAlert('닉네임 중복확인을 완료해주세요.', 'error');
            this.ui.elements.nicknameInput.focus();
            return false;
        }

        return true;
    }

    /**
     * 회원가입 에러 처리
     */
    handleSignupError(error) {
        let errorMessage = error.message || '회원가입 중 오류가 발생했습니다.';

        // 사용자 친화적 메시지 변환
        if (errorMessage.includes('duplicate') || errorMessage.includes('중복')) {
            errorMessage = '이미 존재하는 정보입니다. 이메일이나 닉네임을 확인해주세요.';
        } else if (errorMessage.includes('validation') || errorMessage.includes('형식')) {
            errorMessage = '입력 정보의 형식을 확인해주세요.';
        } else if (errorMessage.includes('network') || errorMessage.includes('네트워크')) {
            errorMessage = '네트워크 연결을 확인하고 다시 시도해주세요.';
        } else if (errorMessage.includes('server') || errorMessage.includes('500')) {
            errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        }

        this.ui.showAlert(errorMessage, 'error');
    }
}