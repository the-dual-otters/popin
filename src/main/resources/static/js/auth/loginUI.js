/**
 * 로그인 UI 관리 클래스
 * 폼 상태 관리, 에러 표시, 로딩 상태 등 UI 관련 기능을 담당
 */
class LoginUI {
    constructor() {
        this.elements = this.initElements();
        this.setupEventListeners();
    }

    /**
     * DOM 요소들 초기화
     * @returns {Object} DOM 요소들
     */
    initElements() {
        return {
            form: document.getElementById('loginForm'),
            button: document.getElementById('loginBtn'),
            alertContainer: document.getElementById('alert-container'),
            emailInput: document.getElementById('email'),
            passwordInput: document.getElementById('password'),
            emailError: document.getElementById('email-error'),
            passwordError: document.getElementById('password-error')
        };
    }

    /**
     * 이벤트 리스너 설정
     */
    setupEventListeners() {
        // 이메일 검증 이벤트
        this.elements.emailInput.addEventListener('blur', () => {
            this.validateEmailField();
        });

        this.elements.emailInput.addEventListener('input', () => {
            this.clearFieldError('email');
        });

        // 비밀번호 검증 이벤트
        this.elements.passwordInput.addEventListener('blur', () => {
            this.validatePasswordField();
        });

        this.elements.passwordInput.addEventListener('input', () => {
            this.clearFieldError('password');
        });

        // Enter 키 처리
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                if (this.elements.button.disabled) {
                    e.preventDefault(); // 버튼이 비활성화일 때만 막음
                }
            }
        });
    }

    /**
     * 이메일 필드 검증 및 UI 업데이트
     * @returns {boolean} 검증 결과
     */
    validateEmailField() {
        const result = LoginValidator.validateEmail(this.elements.emailInput.value);
        if (!result.isValid) {
            this.showFieldError('email', result.message);
            return false;
        }
        this.clearFieldError('email');
        return true;
    }

    /**
     * 비밀번호 필드 검증 및 UI 업데이트
     * @returns {boolean} 검증 결과
     */
    validatePasswordField() {
        const result = LoginValidator.validatePassword(this.elements.passwordInput.value);
        if (!result.isValid) {
            this.showFieldError('password', result.message);
            return false;
        }
        this.clearFieldError('password');
        return true;
    }

    /**
     * 필드 에러 표시
     * @param {string} field 필드명 (email, password)
     * @param {string} message 에러 메시지
     */
    showFieldError(field, message) {
        const input = this.elements[`${field}Input`];
        const error = this.elements[`${field}Error`];

        input.classList.add('error');
        error.textContent = message;
    }

    /**
     * 필드 에러 제거
     * @param {string} field 필드명 (email, password)
     */
    clearFieldError(field) {
        const input = this.elements[`${field}Input`];
        const error = this.elements[`${field}Error`];

        input.classList.remove('error');
        error.textContent = '';
    }

    /**
     * 전체 필드 에러 제거
     */
    clearAllErrors() {
        this.clearFieldError('email');
        this.clearFieldError('password');
    }

    /**
     * 알림 메시지 표시
     * @param {string} message 메시지 내용
     * @param {string} type 메시지 타입 (success, error, info)
     */
    showAlert(message, type = 'error') {
        this.elements.alertContainer.className = `alert alert-${type}`;
        this.elements.alertContainer.textContent = message;
        this.elements.alertContainer.style.display = 'block';

        const hideDelay = type === 'success' ? 3000 : 5000;
        setTimeout(() => {
            this.elements.alertContainer.style.display = 'none';
        }, hideDelay);
    }

    /**
     * 로딩 상태 토글
     * @param {boolean} loading 로딩 여부
     */
    toggleLoading(loading) {
        if (loading) {
            this.elements.button.disabled = true;
            this.elements.button.classList.add('btn-loading');
            this.elements.button.querySelector('.btn-text').textContent = '로그인 중...';
        } else {
            this.elements.button.disabled = false;
            this.elements.button.classList.remove('btn-loading');
            this.elements.button.querySelector('.btn-text').textContent = '로그인';
        }
    }

    /**
     * 폼 데이터 가져오기
     * @returns {FormData} 폼 데이터
     */
    getFormData() {
        return new FormData(this.elements.form);
    }

    /**
     * 폼 리셋
     */
    resetForm() {
        this.elements.form.reset();
        this.clearAllErrors();
    }

    /**
     * 폼 유효성 검사 및 UI 업데이트
     * @returns {boolean} 전체 검증 결과
     */
    validateForm() {
        const emailValid = this.validateEmailField();
        const passwordValid = this.validatePasswordField();

        return emailValid && passwordValid;
    }
}