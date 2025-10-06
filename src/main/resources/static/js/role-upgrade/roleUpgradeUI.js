/**
 * 역할 승격 요청 UI 관리 클래스
 */
class RoleUpgradeUI {
    constructor() {
        this.activeTab = 'host'; // 기본값: 기업 탭
        this.elements = this.getElements();

        this.init();
    }

    /**
     * DOM 요소 가져오기
     */
    getElements() {
        return {
            form: document.getElementById('upgradeForm'),
            tabBtns: document.querySelectorAll('.tab-btn'),

            // 폼 필드들
            companyInput: document.getElementById('company'),
            companyLabel: document.getElementById('company-label'),
            businessNumberInput: document.getElementById('businessNumber'),
            permissionSelect: document.getElementById('permission'),
            additionalTextarea: document.getElementById('additional'),
            businessFileInput: document.getElementById('businessFile'),

            // 그룹 요소들
            roleGroup: document.getElementById('role-group'),

            // 에러 표시 요소들
            companyError: document.getElementById('company-error'),
            businessNumberError: document.getElementById('businessNumber-error'),
            permissionError: document.getElementById('permission-error'),
            additionalError: document.getElementById('additional-error'),
            fileError: document.getElementById('file-error'),

            // 기타
            alertContainer: document.getElementById('alert-container'),
            fileNameDisplay: document.getElementById('file-name'),
            submitBtn: document.getElementById('upgradeBtn')
        };
    }

    /**
     * UI 초기화
     */
    init() {
        this.setupTabSwitching();
        this.setupFormValidation();
        this.setupFileUpload();

        // 초기 상태 설정 (기업 탭)
        this.switchTab('host');
    }

    /**
     * 탭 전환 이벤트 설정
     */
    setupTabSwitching() {
        this.elements.tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.getAttribute('data-tab');
                this.switchTab(tab);
            });
        });
    }

    /**
     * 폼 유효성 검사 이벤트 설정
     */
    setupFormValidation() {
        // 회사명/공간 표시명 입력
        if (this.elements.companyInput) {
            this.elements.companyInput.addEventListener('blur', () => {
                this.validateField('company');
            });
        }

        // 사업자등록번호 입력
        if (this.elements.businessNumberInput) {
            this.elements.businessNumberInput.addEventListener('input', (e) => {
                // 숫자와 하이픈만 허용
                e.target.value = e.target.value.replace(/[^0-9-]/g, '');
            });

            this.elements.businessNumberInput.addEventListener('blur', () => {
                this.validateField('businessNumber');
            });
        }

        // 권한 선택
        if (this.elements.permissionSelect) {
            this.elements.permissionSelect.addEventListener('change', () => {
                this.validateField('permission');
            });
        }

        // 추가 작성 사항
        if (this.elements.additionalTextarea) {
            this.elements.additionalTextarea.addEventListener('blur', () => {
                this.validateField('additional');
            });
        }

        // 파일 업로드
        if (this.elements.businessFileInput) {
            this.elements.businessFileInput.addEventListener('change', (e) => {
                this.handleFileSelect(e.target.files[0]);
            });
        }
    }

    /**
     * 탭 전환 (수정된 로직)
     * @param {string} tab 탭 이름 ('host' 또는 'guest')
     *
     * 탭 매핑:
     * - 'host' 탭 = 기업 (PROVIDER 역할)
     * - 'guest' 탭 = 공간 제공자 (HOST 역할)
     */
    switchTab(tab) {
        this.activeTab = tab;

        if (tab === 'host') {
            // 기업 탭
            this.elements.companyLabel.textContent = '회사명';
            this.elements.companyInput.placeholder = '회사명을 입력해 주세요';
            this.hideRoleField(); // 기업은 권한 필드가 없음
        } else if (tab === 'guest') {
            // 공간 제공자 탭
            this.elements.companyLabel.textContent = '공간 표시명';
            this.elements.companyInput.placeholder = '공간 표시명을 입력해 주세요';
            this.showRoleField(); // 공간 제공자는 권한 필드 필요
        }

        // 탭 활성화 토글
        this.elements.tabBtns.forEach(btn => btn.classList.remove('active'));
        const activeBtn = Array.from(this.elements.tabBtns).find(btn => btn.dataset.tab === tab);
        if (activeBtn) activeBtn.classList.add('active');
    }

    /**
     * 권한 필드 표시
     */
    showRoleField() {
        if (this.elements.roleGroup) {
            this.elements.roleGroup.classList.remove('hidden');
        }
    }

    /**
     * 권한 필드 숨기기
     */
    hideRoleField() {
        if (this.elements.roleGroup) {
            this.elements.roleGroup.classList.add('hidden');
        }
        // 권한 선택 초기화
        if (this.elements.permissionSelect) {
            this.elements.permissionSelect.value = '';
        }
    }

    /**
     * 파일 업로드 설정
     */
    setupFileUpload() {
        // 파일 선택 영역 클릭 이벤트
        const fileLabel = document.querySelector('.file-label');
        if (fileLabel) {
            fileLabel.addEventListener('click', () => {
                this.elements.businessFileInput.click();
            });
        }
    }

    /**
     * 파일 선택 처리
     */
    handleFileSelect(file) {
        if (!file) {
            if (this.elements.fileNameDisplay) {
                this.elements.fileNameDisplay.textContent = '';
            }
            this.clearFieldError('file');
            return;
        }

        // 파일 검증 (이제 선택사항이므로 에러가 나면 표시만)
        if (typeof RoleUpgradeValidator !== 'undefined') {
            const validation = RoleUpgradeValidator.validateFile(file);
            if (!validation.isValid) {
                this.showFieldError('file', validation.message);
                this.elements.businessFileInput.value = '';
                if (this.elements.fileNameDisplay) {
                    this.elements.fileNameDisplay.textContent = '';
                }
                return;
            }
        }

        // 파일명 표시
        if (this.elements.fileNameDisplay) {
            this.elements.fileNameDisplay.textContent = file.name;
        }
        this.clearFieldError('file');
    }

    /**
     * 필드별 유효성 검사
     */
    validateField(fieldName) {
        const value = this.getFieldValue(fieldName);

        if (typeof RoleUpgradeValidator !== 'undefined') {
            const result = RoleUpgradeValidator.validateField(fieldName, value, this.activeTab);

            if (result.isValid) {
                this.clearFieldError(fieldName);
            } else {
                this.showFieldError(fieldName, result.message);
            }

            return result.isValid;
        }

        return true;
    }

    /**
     * 필드 값 가져오기
     */
    getFieldValue(fieldName) {
        switch (fieldName) {
            case 'company':
                return this.elements.companyInput?.value?.trim() || '';
            case 'businessNumber':
                return this.elements.businessNumberInput?.value?.trim() || '';
            case 'permission':
                return this.elements.permissionSelect?.value || '';
            case 'additional':
                return this.elements.additionalTextarea?.value?.trim() || '';
            default:
                return '';
        }
    }

    /**
     * 전체 폼 유효성 검사
     */
    validateForm() {
        const errors = {};

        // 기본 필수 필드
        const requiredFields = ['company', 'businessNumber'];

        // 공간 제공자(guest) 탭에서만 권한 필수
        if (this.activeTab === 'guest') {
            requiredFields.push('permission');
        }

        let hasErrors = false;

        requiredFields.forEach(field => {
            const value = this.getFieldValue(field);

            if (typeof RoleUpgradeValidator !== 'undefined') {
                const validation = RoleUpgradeValidator.validateField(field, value, this.activeTab);
                if (!validation.isValid) {
                    errors[field] = validation.message;
                    this.showFieldError(field, validation.message);
                    hasErrors = true;
                } else {
                    this.clearFieldError(field);
                }
            }
        });

        return !hasErrors;
    }

    /**
     * 승격 요청 데이터 가져오기
     * 수정된 역할 매핑 로직
     */
    getUpgradeRequestData() {
        const isSpaceProvider = this.activeTab === 'guest'; // 공간 제공자 탭 여부

        // 폼 값을 payload로 모으기
        const payload = {
            company: this.getFieldValue('company'),
            businessNumber: this.getFieldValue('businessNumber'),
            additional: this.getFieldValue('additional'),
            ...(isSpaceProvider ? { permission: this.getFieldValue('permission') } : {})
        };

        // 수정된 역할 매핑:
        // - 기업(host 탭) → PROVIDER 역할
        // - 공간 제공자(guest 탭) → HOST 역할
        return {
            requestedRole: isSpaceProvider ? 'HOST' : 'PROVIDER',
            payload: JSON.stringify(payload)
        };
    }

    /**
     * 선택된 파일 가져오기
     */
    getSelectedFile() {
        const fileInput = this.elements.businessFileInput;
        return fileInput && fileInput.files && fileInput.files.length > 0 ? fileInput.files[0] : null;
    }

    /**
     * 필드 에러 표시
     */
    showFieldError(fieldName, message) {
        const errorElement = this.elements[`${fieldName}Error`];
        const inputElement = this.elements[`${fieldName}Input`] || this.elements[`${fieldName}Select`] || this.elements[`${fieldName}Textarea`];

        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }

        if (inputElement) {
            inputElement.classList.add('error');
        }
    }

    /**
     * 필드 에러 지우기
     */
    clearFieldError(fieldName) {
        const errorElement = this.elements[`${fieldName}Error`];
        const inputElement = this.elements[`${fieldName}Input`] || this.elements[`${fieldName}Select`] || this.elements[`${fieldName}Textarea`];

        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }

        if (inputElement) {
            inputElement.classList.remove('error');
        }
    }

    /**
     * 알림 메시지 표시
     */
    showAlert(message, type = 'info') {
        if (!this.elements.alertContainer) return;

        const alertContainer = this.elements.alertContainer;
        alertContainer.textContent = message;
        alertContainer.className = `alert ${type}`;
        alertContainer.style.display = 'block';

        // 3초 후 자동 숨김
        setTimeout(() => {
            alertContainer.style.display = 'none';
        }, 3000);
    }

    /**
     * 로딩 상태 토글
     */
    toggleLoading(isLoading) {
        if (!this.elements.submitBtn) return;

        const loadingElement = this.elements.submitBtn.querySelector('.loading');
        const textElement = this.elements.submitBtn.querySelector('.btn-text');

        if (isLoading) {
            this.elements.submitBtn.disabled = true;
            this.elements.submitBtn.classList.add('loading');
            if (textElement) textElement.textContent = '처리 중...';
        } else {
            this.elements.submitBtn.disabled = false;
            this.elements.submitBtn.classList.remove('loading');
            if (textElement) textElement.textContent = '요청하기';
        }
    }

    /**
     * 폼 초기화
     */
    resetForm() {
        if (this.elements.form) {
            this.elements.form.reset();
        }

        // 파일명 표시 초기화
        if (this.elements.fileNameDisplay) {
            this.elements.fileNameDisplay.textContent = '';
        }

        // 모든 에러 메시지 지우기
        ['company', 'businessNumber', 'permission', 'additional', 'file'].forEach(field => {
            this.clearFieldError(field);
        });

        // 알림 숨기기
        if (this.elements.alertContainer) {
            this.elements.alertContainer.style.display = 'none';
        }
    }

    /**
     * 탭별 필수 필드 반환
     */
    getRequiredFields() {
        const requiredFields = ['company', 'businessNumber'];

        // 공간 제공자(guest) 탭에서만 권한 필수
        if (this.activeTab === 'guest') {
            requiredFields.push('permission');
        }

        return requiredFields;
    }

    /**
     * 현재 활성 탭 반환
     */
    getActiveTab() {
        return this.activeTab;
    }

    /**
     * 현재 선택된 역할 반환
     */
    getSelectedRole() {
        // 공간 제공자(guest 탭) → HOST, 기업(host 탭) → PROVIDER
        return this.activeTab === 'guest' ? 'HOST' : 'PROVIDER';
    }
}