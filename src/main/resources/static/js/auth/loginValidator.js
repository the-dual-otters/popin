/**
 * 로그인 폼 검증 유틸리티
 * 이메일과 비밀번호 유효성 검사를 담당
 */
class LoginValidator {

    /**
     * 이메일 검증
     * @param {string} email 이메일 주소
     * @returns {Object} { isValid: boolean, message: string }
     */
    static validateEmail(email) {
        const trimmedEmail = email.trim();

        if (!trimmedEmail) {
            return { isValid: false, message: '이메일을 입력해주세요.' };
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(trimmedEmail)) {
            return { isValid: false, message: '올바른 이메일 형식을 입력해주세요.' };
        }

        if (trimmedEmail.length > 100) {
            return { isValid: false, message: '이메일은 100자를 초과할 수 없습니다.' };
        }

        return { isValid: true, message: '' };
    }

    /**
     * 비밀번호 검증 (로그인용 - 간단한 검증)
     * @param {string} password 비밀번호
     * @returns {Object} { isValid: boolean, message: string }
     */
    static validatePassword(password) {
        if (!password) {
            return { isValid: false, message: '비밀번호를 입력해주세요.' };
        }

        if (password.length < 1) {
            return { isValid: false, message: '비밀번호를 입력해주세요.' };
        }

        // 로그인 시에는 복잡한 검증보다는 단순하게 처리
        // 실제 검증은 서버에서 수행
        return { isValid: true, message: '' };
    }

    /**
     * 폼 전체 검증
     * @param {FormData} formData 폼 데이터
     * @returns {Object} { isValid: boolean, errors: Object }
     */
    static validateForm(formData) {
        const email = String(formData.get('email') ?? '').trim();
        const password = String(formData.get('password') ?? '');

        const emailResult = this.validateEmail(email);
        const passwordResult = this.validatePassword(password);

        const errors = {};
        if (!emailResult.isValid) {
            errors.email = emailResult.message;
        }
        if (!passwordResult.isValid) {
            errors.password = passwordResult.message;
        }

        return {
            isValid: emailResult.isValid && passwordResult.isValid,
            errors
        };
    }
}