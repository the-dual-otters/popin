/**
 * 회원가입 폼 UI 관리 클래스
 */
class SignupUI {
    constructor() {
        this.elements = this.initializeElements();
        this.selectedTags = new Set();
        this.categories = [];
        this.validationStates = {
            email: false,
            nickname: false
        };
        this.init();
    }

    /**
     * DOM 요소 초기화
     */
    initializeElements() {
        return {
            form: document.getElementById('signupForm'),
            nameInput: document.getElementById('name'),
            nicknameInput: document.getElementById('nickname'),
            emailInput: document.getElementById('email'),
            passwordInput: document.getElementById('password'),
            passwordConfirmInput: document.getElementById('passwordConfirm'),
            phoneInput: document.getElementById('phone'),
            nicknameCheckBtn: document.getElementById('nicknameCheckBtn'),
            emailCheckBtn: document.getElementById('emailCheckBtn'),
            submitBtn: document.getElementById('signupBtn'),
            requirements: {
                length: document.getElementById('req-length'),
                upperLower: document.getElementById('req-upper-lower'),
                number: document.getElementById('req-number'),
                special: document.getElementById('req-special')
            },
            categoryContainer: document.querySelector('.interest-categories'),
            selectedTagsContainer: document.getElementById('selectedTags')
        };
    }

    /**
     * UI 초기화
     */
    async init() {
        this.addAlertAnimations();
        this.setupEventListeners();
        await this.loadCategories();
        this.updateSubmitButton();
    }

    /**
     * 알림 애니메이션 CSS 추가
     */
    addAlertAnimations() {
        if (!document.getElementById('alert-animations')) {
            const style = document.createElement('style');
            style.id = 'alert-animations';
            style.textContent = `
                @keyframes slideDown {
                    from { opacity: 0; transform: translateX(-50%) translateY(-20px); }
                    to { opacity: 1; transform: translateX(-50%) translateY(0); }
                }
                @keyframes slideUp {
                    from { opacity: 1; transform: translateX(-50%) translateY(0); }
                    to { opacity: 0; transform: translateX(-50%) translateY(-20px); }
                }
            `;
            document.head.appendChild(style);
        }
    }

    /**
     * 카테고리 데이터 로드 및 UI 생성
     */
    async loadCategories() {
        try {
            const signupApi = new SignupApi();
            this.categories = await signupApi.getAllCategories();

            if (this.categories?.length > 0) {
                this.buildCategoryUI();
            } else {
                this.setupFallbackCategories();
                this.showAlert('기본 카테고리를 사용합니다.', 'warning');
            }
        } catch (error) {
            console.error('카테고리 로드 실패:', error);
            this.setupFallbackCategories();
            this.showAlert('기본 카테고리를 사용합니다.', 'warning');
        }
    }

    /**
     * 카테고리 UI 동적 생성
     */
    buildCategoryUI() {
        this.elements.categoryContainer.innerHTML = '';

        this.categories.forEach(category => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'category-btn';
            button.setAttribute('data-category-name', category.name);
            button.textContent = category.name;

            button.addEventListener('click', (e) => {
                e.preventDefault();
                this.handleCategoryClick(category.name, button);
            });

            this.elements.categoryContainer.appendChild(button);
        });
    }

    /**
     * 카테고리 클릭 처리
     */
    handleCategoryClick(categoryName, buttonElement) {
        if (this.selectedTags.has(categoryName)) {
            this.selectedTags.delete(categoryName);
            buttonElement.classList.remove('selected');
        } else {
            if (this.selectedTags.size >= 10) {
                this.showAlert('관심사는 최대 10개까지 선택 가능합니다.', 'warning');
                return;
            }
            this.selectedTags.add(categoryName);
            buttonElement.classList.add('selected');
        }

        this.updateSelectedTagsDisplay();
        this.updateSubmitButton();
    }

    /**
     * 폴백 카테고리 설정 (API 실패 시)
     */
    setupFallbackCategories() {
        this.categories = [
            { name: '패션' }, { name: '반려동물' }, { name: '게임' },
            { name: '캐릭터/IP' }, { name: '문화/컨텐츠' },
            { name: '연예' }, { name: '여행/레저/스포츠' }
        ];
        this.buildCategoryUI();
    }

    /**
     * 이벤트 리스너 설정
     */
    setupEventListeners() {
        // 입력 필드 이벤트
        this.elements.nameInput?.addEventListener('input', () => this.validateField('name'));
        this.elements.nicknameInput?.addEventListener('input', () => this.validateField('nickname'));
        this.elements.emailInput?.addEventListener('input', () => this.validateField('email'));
        this.elements.passwordInput?.addEventListener('input', () => this.validatePasswordField());
        this.elements.passwordConfirmInput?.addEventListener('input', () => this.validatePasswordConfirmField());
        this.elements.phoneInput?.addEventListener('input', () => this.validateField('phone'));

        // 중복확인 버튼
        this.elements.nicknameCheckBtn?.addEventListener('click', () => this.validateField('nickname', true));
        this.elements.emailCheckBtn?.addEventListener('click', () => this.validateField('email', true));

        // 폼 제출 방지
        this.elements.form?.addEventListener('submit', e => e.preventDefault());
    }

    /**
     * 통합된 필드 검증 메서드
     */
    async validateField(fieldName, checkDuplicate = false) {
        const input = this.elements[`${fieldName}Input`];
        if (!input) return false;

        const value = input.value.trim();
        let result;

        // 기본 유효성 검사
        switch (fieldName) {
            case 'name':
                result = SignupValidator.validateName(value);
                break;
            case 'nickname':
                result = SignupValidator.validateNickname(value);
                break;
            case 'email':
                result = SignupValidator.validateEmail(value);
                break;
            case 'phone':
                result = SignupValidator.validatePhone(value);
                break;
            default:
                return false;
        }

        // 기본 검증 실패 시
        if (!result.isValid) {
            this.showFieldError(fieldName, result.message);
            if (fieldName === 'nickname' || fieldName === 'email') {
                this.validationStates[fieldName] = false;
            }
            this.updateSubmitButton();
            return false;
        }

        // 중복 확인이 필요한 경우
        if (checkDuplicate && (fieldName === 'nickname' || fieldName === 'email')) {
            return await this.checkDuplicate(fieldName, value);
        }

        // 기본 검증 통과 시 에러 클리어
        this.clearFieldError(fieldName);
        this.updateSubmitButton();
        return true;
    }

    /**
     * 중복 확인 처리
     */
    async checkDuplicate(fieldName, value) {
        const btnId = `${fieldName}CheckBtn`;
        this.setButtonLoading(btnId, true);

        try {
            const signupApi = new SignupApi();
            const result = fieldName === 'nickname'
                ? await signupApi.validateNickname(value)
                : await signupApi.validateEmail(value);

            if (result.isValid) {
                this.showFieldSuccess(fieldName, result.message);
                this.validationStates[fieldName] = true;
            } else {
                this.showFieldError(fieldName, result.message);
                this.validationStates[fieldName] = false;
            }
        } catch (error) {
            this.showFieldError(fieldName, error.message);
            this.validationStates[fieldName] = false;
        } finally {
            this.setButtonLoading(btnId, false);
            this.updateSubmitButton();
        }

        return this.validationStates[fieldName];
    }

    /**
     * 비밀번호 필드 검증
     */
    validatePasswordField() {
        const password = this.elements.passwordInput.value;
        const result = SignupValidator.validatePassword(password);

        // 비밀번호 요구사항 업데이트
        if (result.requirements) {
            Object.keys(result.requirements).forEach(key => {
                const element = this.elements.requirements[key];
                if (element) {
                    element.classList.toggle('valid', result.requirements[key].valid);
                    element.classList.toggle('invalid', !result.requirements[key].valid);
                }
            });
        }

        if (!result.isValid) {
            this.showFieldError('password', result.message);
        } else {
            this.clearFieldError('password');
        }

        this.validatePasswordConfirmField();
        this.updateSubmitButton();
        return result.isValid;
    }

    /**
     * 비밀번호 확인 필드 검증
     */
    validatePasswordConfirmField() {
        const password = this.elements.passwordInput.value;
        const passwordConfirm = this.elements.passwordConfirmInput.value;
        const result = SignupValidator.validatePasswordConfirm(password, passwordConfirm);

        if (!result.isValid) {
            this.showFieldError('passwordConfirm', result.message);
        } else {
            this.clearFieldError('passwordConfirm');
        }

        this.updateSubmitButton();
        return result.isValid;
    }

    /**
     * 선택된 태그 표시 업데이트
     */
    updateSelectedTagsDisplay() {
        if (!this.elements.selectedTagsContainer) return;

        if (this.selectedTags.size === 0) {
            this.elements.selectedTagsContainer.innerHTML =
                '<span class="no-selection">선택된 관심사가 없습니다.</span>';
            return;
        }

        this.elements.selectedTagsContainer.innerHTML = '';
        this.selectedTags.forEach(tag => {
            const tagElement = document.createElement('span');
            tagElement.className = 'selected-tag';
            tagElement.innerHTML = `${tag}<button type="button" class="remove-tag" data-tag="${tag}">&times;</button>`;

            tagElement.querySelector('.remove-tag').addEventListener('click', (e) => {
                e.preventDefault();
                this.removeSelectedTag(tag);
            });

            this.elements.selectedTagsContainer.appendChild(tagElement);
        });
    }

    /**
     * 선택된 태그 제거
     */
    removeSelectedTag(tag) {
        this.selectedTags.delete(tag);

        const categoryBtn = this.elements.categoryContainer.querySelector(`[data-category-name="${tag}"]`);
        categoryBtn?.classList.remove('selected');

        this.updateSelectedTagsDisplay();
        this.updateSubmitButton();
    }

    /**
     * 필드 에러 표시
     */
    showFieldError(fieldName, message) {
        const errorElement = document.getElementById(`${fieldName}-error`);
        const inputElement = document.getElementById(fieldName);

        if (errorElement) {
            errorElement.textContent = message;
            errorElement.className = 'field-error show';
        }

        if (inputElement) {
            inputElement.classList.add('error');
            inputElement.classList.remove('success');
        }
    }

    /**
     * 필드 성공 표시
     */
    showFieldSuccess(fieldName, message) {
        const errorElement = document.getElementById(`${fieldName}-error`);
        const inputElement = document.getElementById(fieldName);

        if (errorElement) {
            errorElement.textContent = message;
            errorElement.className = 'field-error success show';
        }

        if (inputElement) {
            inputElement.classList.add('success');
            inputElement.classList.remove('error');
        }
    }

    /**
     * 필드 에러 클리어
     */
    clearFieldError(fieldName) {
        const errorElement = document.getElementById(`${fieldName}-error`);
        const inputElement = document.getElementById(fieldName);

        if (errorElement) {
            errorElement.textContent = '';
            errorElement.className = 'field-error';
        }

        if (inputElement) {
            inputElement.classList.remove('error', 'success');
        }
    }

    /**
     * 버튼 로딩 상태 설정
     */
    setButtonLoading(buttonId, loading) {
        const button = document.getElementById(buttonId);
        if (!button) return;

        if (loading) {
            button.disabled = true;
            button.innerHTML = '<span class="loading"></span>확인 중...';
        } else {
            button.disabled = false;
            button.textContent = '중복확인';
        }
    }

    /**
     * 알림 메시지 표시
     */
    showAlert(message, type = 'info') {
        // 기존 알림 제거
        document.querySelector('.alert-fixed')?.remove();

        // 새 알림 생성
        const alert = document.createElement('div');
        alert.className = `alert-fixed alert-${type}`;
        alert.textContent = message;

        // 스타일 설정
        const colors = {
            success: { bg: '#d4edda', text: '#155724', border: '#28a745' },
            error: { bg: '#f8d7da', text: '#721c24', border: '#dc3545' },
            warning: { bg: '#fff3cd', text: '#856404', border: '#ffc107' },
            info: { bg: '#d1ecf1', text: '#0c5460', border: '#17a2b8' }
        };

        const color = colors[type] || colors.info;
        alert.style.cssText = `
            position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
            max-width: 90%; width: 400px; padding: 14px 18px; border-radius: 8px;
            font-size: 14px; font-weight: 500; z-index: 9999;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15); animation: slideDown 0.3s ease;
            background-color: ${color.bg}; color: ${color.text}; border-left: 4px solid ${color.border};
        `;

        document.body.appendChild(alert);

        // 3초 후 자동 제거
        setTimeout(() => {
            if (alert.parentNode) {
                alert.style.animation = 'slideUp 0.3s ease';
                setTimeout(() => alert.remove(), 300);
            }
        }, 3000);
    }

    /**
     * 제출 버튼 상태 업데이트
     */
    updateSubmitButton() {
        if (!this.elements.submitBtn) return;
        this.elements.submitBtn.disabled = !this.validateForm();
    }

    /**
     * 폼 전체 검증
     */
    validateForm() {
        const formData = this.getFormData();
        const selectedTags = Array.from(this.selectedTags);

        // FormData 검증
        const formValidation = SignupValidator.validateForm(formData, selectedTags);
        if (!formValidation.isValid) return false;

        // 중복확인 상태 검증
        const email = formData.get('email').trim();
        const nickname = formData.get('nickname').trim();

        return (!email || this.validationStates.email) &&
            (!nickname || this.validationStates.nickname) &&
            this.validateBasicFields();
    }

    /**
     * 기본 필드들 검증 (중복확인 제외)
     */
    validateBasicFields() {
        const formData = this.getFormData();
        const fields = ['name', 'password', 'passwordConfirm', 'phone'];

        return fields.every(field => {
            const value = field === 'password' || field === 'passwordConfirm'
                ? formData.get(field)
                : formData.get(field).trim();

            if (field === 'passwordConfirm') {
                return SignupValidator.validatePasswordConfirm(
                    formData.get('password'), value
                ).isValid;
            }

            return SignupValidator[`validate${field.charAt(0).toUpperCase() + field.slice(1)}`](value).isValid;
        });
    }

    /**
     * 폼 데이터 가져오기
     */
    getFormData() {
        return new FormData(this.elements.form);
    }

    /**
     * 선택된 태그 배열 반환
     */
    getSelectedTags() {
        return Array.from(this.selectedTags);
    }

    /**
     * 로딩 상태 토글
     */
    toggleLoading(loading) {
        if (!this.elements.submitBtn) return;

        const loadingElement = this.elements.submitBtn.querySelector('.loading');
        const textElement = this.elements.submitBtn.querySelector('.btn-text');

        if (loading) {
            this.elements.submitBtn.disabled = true;
            if (loadingElement) loadingElement.style.display = 'inline-block';
            if (textElement) textElement.textContent = '가입 중...';
        } else {
            this.elements.submitBtn.disabled = false;
            if (loadingElement) loadingElement.style.display = 'none';
            if (textElement) textElement.textContent = '가입하기';
        }
    }

    /**
     * 폼 리셋
     */
    resetForm() {
        this.elements.form?.reset();

        // 선택된 태그 초기화
        this.selectedTags.clear();
        this.updateSelectedTagsDisplay();

        // 검증 상태 초기화
        this.validationStates = { email: false, nickname: false };

        // 에러 메시지 클리어
        document.querySelectorAll('.field-error').forEach(element => {
            element.textContent = '';
            element.className = 'field-error';
        });

        // 입력 필드 상태 클리어
        document.querySelectorAll('.form-input').forEach(element => {
            element.classList.remove('error', 'success');
        });

        // 비밀번호 요구사항 초기화
        Object.values(this.elements.requirements).forEach(element => {
            element?.classList.remove('valid', 'invalid');
        });

        this.updateSubmitButton();
    }
}