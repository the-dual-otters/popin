/**
 * 유저 승격 요청 페이지 메인 컨트롤러
 */
class RoleUpgradeController {
    constructor() {
        // API와 UI 인스턴스 생성 전에 존재 여부 확인
        this.api = typeof RoleUpgradeApi !== 'undefined' ? new RoleUpgradeApi() : null;
        this.ui = typeof RoleUpgradeUI !== 'undefined' ? new RoleUpgradeUI() : null;

        // 요청 완료 상태 관리
        this.isRequestCompleted = false;
        this.submittedData = null;

        this.init();
    }

    /**
     * 컨트롤러 초기화
     */
    init() {
        // API와 UI가 로드되지 않았다면 에러 처리
        if (!this.api || !this.ui) {
            console.error('RoleUpgradeApi 또는 RoleUpgradeUI 클래스를 찾을 수 없습니다.');
            this.showSimpleAlert('페이지 로딩 중 오류가 발생했습니다. 새로고침해주세요.');
            return;
        }

        // 약간의 지연을 두고 초기화 (DOM 요소들이 완전히 로드된 후)
        setTimeout(() => {
            this.checkUserAuth();
            this.setupFormSubmission();
        }, 100);
    }

    /**
     * 사용자 인증 확인
     */
    async checkUserAuth() {
        if (!this.api) return;

        const token = this.api.getStoredToken();

        if (!token) {
            this.showSimpleAlert('로그인이 필요합니다.');
            setTimeout(() => {
                window.location.href = '/templates/pages/auth/login.html';
            }, 2000);
            return;
        }

        // 기존 요청 확인
        try {
            await this.checkExistingRequest();
        } catch (error) {
            console.warn('기존 요청 확인 실패:', error);
        }
    }

    /**
     * 기존 승격 요청 확인
     */
    async checkExistingRequest() {
        if (!this.api || !this.ui) return;

        try {
            const requests = await this.api.getMyUpgradeRequests();
            const pendingRequest = requests.find(req => req.status === 'PENDING');

            if (pendingRequest) {
                this.isRequestCompleted = true;

                // 폼에 기존 데이터 채우기
                this.fillFormWithData(pendingRequest);

                // 폼 비활성화
                this.disableForm();

                // 성공 메시지 표시
                this.ui.showAlert('이미 제출된 승격 요청이 있습니다. 관리자 검토를 기다려주세요.', 'info');

                // UI 요소가 존재할 때만 접근
                if (this.ui.elements && this.ui.elements.submitBtn) {
                    this.ui.elements.submitBtn.disabled = true;
                    const btnText = this.ui.elements.submitBtn.querySelector('.btn-text');
                    if (btnText) {
                        btnText.textContent = '처리 대기 중';
                    }
                }
            }
        } catch (error) {
            console.warn('기존 요청 확인 중 오류:', error);
        }
    }

    /**
     * 요청 정보를 화면에 표시 (폼만 유지하고 별도 카드는 표시하지 않음)
     */
    displayRequestInfo(requestData) {
        if (!this.ui) return;

        // 폼에 기존 데이터 채우기
        this.fillFormWithData(requestData);

        // 폼 비활성화
        this.disableForm();

        // 성공 메시지 표시
        this.ui.showAlert('승격 요청이 성공적으로 제출되었습니다! 관리자 검토를 기다려주세요.', 'success');
    }

    /**
     * 폼에 제출된 데이터 채우기
     */
    fillFormWithData(requestData) {
        if (!requestData.payload) return;

        try {
            const payload = typeof requestData.payload === 'string'
                ? JSON.parse(requestData.payload)
                : requestData.payload;

            console.log('폼에 채울 데이터:', payload); // 디버깅용
            console.log('요청된 역할:', requestData.requestedRole); // 디버깅용

            // 탭 상태 먼저 설정 (다른 필드들보다 먼저 설정해야 함)
            if (requestData.requestedRole) {
                // 역할에 따른 탭 설정 수정
                const targetTab = this.getTabByRole(requestData.requestedRole);
                console.log('설정할 탭:', targetTab); // 디버깅용

                const tabBtn = document.querySelector(`[data-tab="${targetTab}"]`);

                if (tabBtn && !tabBtn.classList.contains('active')) {
                    // 탭 클릭 이벤트 시뮬레이션
                    tabBtn.click();

                    // 탭 전환 후 잠시 대기 (UI 업데이트를 위해)
                    setTimeout(() => {
                        this.setFormValues(payload);
                    }, 100);
                } else {
                    this.setFormValues(payload);
                }
            } else {
                this.setFormValues(payload);
            }

        } catch (error) {
            console.warn('폼 데이터 채우기 실패:', error);
        }
    }

    /**
     * 역할에 따른 탭 반환 (수정된 로직)
     */
    getTabByRole(requestedRole) {
        // PROVIDER = 기업 = 'host' 탭
        // HOST = 공간 제공자 = 'guest' 탭
        if (requestedRole === 'PROVIDER') {
            return 'host'; // 기업 탭
        } else if (requestedRole === 'HOST') {
            return 'guest'; // 공간 제공자 탭
        }
        return 'host'; // 기본값
    }

    /**
     * 실제 폼 값 설정
     */
    setFormValues(payload) {
        // 회사명/공간 표시명
        const companyInput = document.getElementById('company');
        if (companyInput && payload.company) {
            companyInput.value = payload.company;
            console.log('회사명 설정:', payload.company);
        }

        // 사업자등록번호
        const businessNumberInput = document.getElementById('businessNumber');
        if (businessNumberInput && payload.businessNumber) {
            businessNumberInput.value = payload.businessNumber;
            console.log('사업자등록번호 설정:', payload.businessNumber);
        }

        // 권한 (공간 제공자인 경우)
        const permissionSelect = document.getElementById('permission');
        if (permissionSelect && payload.permission) {
            permissionSelect.value = payload.permission;
            console.log('권한 설정:', payload.permission);
        }

        // 추가 작성 사항
        const additionalTextarea = document.getElementById('additional');
        if (additionalTextarea && payload.additional) {
            additionalTextarea.value = payload.additional;
            console.log('추가 작성사항 설정:', payload.additional);
        }
    }

    /**
     * 폼 비활성화
     */
    disableForm() {
        const form = document.getElementById('upgradeForm');
        if (!form) return;

        // 모든 입력 필드 비활성화
        const inputs = form.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.disabled = true;
            input.style.backgroundColor = '#f5f5f5';
            input.style.color = '#666';
        });

        // 탭 버튼 비활성화
        const tabBtns = document.querySelectorAll('.tab-btn');
        tabBtns.forEach(btn => {
            btn.disabled = true;
            btn.style.opacity = '0.6';
            btn.style.cursor = 'not-allowed';
        });

        // 파일 업로드 영역 비활성화
        const fileUpload = document.querySelector('.file-upload-area');
        if (fileUpload) {
            fileUpload.style.opacity = '0.6';
            fileUpload.style.pointerEvents = 'none';
        }
    }

    /**
     * 요청 상태 정보 표시
     */
    displayRequestStatus(requestData) {
        // 기존 상태 정보가 있다면 제거
        const existingStatus = document.getElementById('request-status-info');
        if (existingStatus) {
            existingStatus.remove();
        }

        // 상태 정보 HTML 생성
        const statusHtml = `
            <div id="request-status-info" class="request-status-card">
                <h3>제출된 요청 정보</h3>
                <div class="status-info">
                    <div class="status-item">
                        <span class="label">요청 ID:</span>
                        <span class="value">#${requestData.id}</span>
                    </div>
                    <div class="status-item">
                        <span class="label">요청 역할:</span>
                        <span class="value">${this.getRoleDisplayName(requestData.requestedRole)}</span>
                    </div>
                    <div class="status-item">
                        <span class="label">상태:</span>
                        <span class="value status-${requestData.status.toLowerCase()}">${this.getStatusText(requestData.status)}</span>
                    </div>
                    <div class="status-item">
                        <span class="label">제출일:</span>
                        <span class="value">${this.formatDate(requestData.createdAt)}</span>
                    </div>
                </div>
                <div class="status-actions">
                    <button type="button" class="btn-secondary" onclick="window.location.href='/templates/pages/main.html'">
                        메인으로 돌아가기
                    </button>
                </div>
            </div>
        `;

        // 폼 컨테이너 아래에 상태 정보 추가
        const formContainer = document.querySelector('.form-container');
        if (formContainer) {
            formContainer.insertAdjacentHTML('afterend', statusHtml);
        }
    }

    /**
     * 역할 표시명 반환
     */
    getRoleDisplayName(role) {
        switch (role) {
            case 'PROVIDER':
                return '기업';
            case 'HOST':
                return '공간 제공자';
            default:
                return role;
        }
    }

    /**
     * 상태 텍스트 변환
     */
    getStatusText(status) {
        switch (status) {
            case 'PENDING':
                return '검토 대기';
            case 'APPROVED':
                return '승인 완료';
            case 'REJECTED':
                return '반려';
            default:
                return status;
        }
    }

    /**
     * 날짜 포맷팅
     */
    formatDate(dateString) {
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (error) {
            return dateString;
        }
    }

    /**
     * 폼 제출 이벤트 설정
     */
    setupFormSubmission() {
        if (!this.ui || !this.ui.elements || !this.ui.elements.form) {
            console.warn('폼을 찾을 수 없습니다.');
            return;
        }

        this.ui.elements.form.addEventListener('submit', (e) => {
            this.handleSubmit(e);
        });
    }

    /**
     * 폼 제출 처리
     */
    async handleSubmit(e) {
        e.preventDefault();

        // 이미 요청이 완료된 상태라면 제출 방지
        if (this.isRequestCompleted) {
            this.ui.showAlert('이미 제출된 요청이 있습니다.', 'info');
            return;
        }

        if (!this.ui || !this.api) {
            this.showSimpleAlert('시스템 오류가 발생했습니다.');
            return;
        }

        // 클라이언트 검증
        if (!this.ui.validateForm()) {
            this.ui.showAlert('입력 정보를 확인해주세요.', 'error');
            return;
        }

        // 요청 데이터 준비
        const requestData = this.ui.getUpgradeRequestData();
        const selectedFile = this.ui.getSelectedFile();

        // 최종 검증
        if (!this.validateSubmissionData(requestData, selectedFile)) {
            return;
        }

        // 제출된 데이터 저장 (성공 시 사용)
        this.submittedData = {
            ...requestData,
            file: selectedFile
        };

        this.ui.toggleLoading(true);

        try {
            const response = await this.api.createUpgradeRequest(requestData, selectedFile);

            if (response.success) {
                // 요청 완료 상태로 설정
                this.isRequestCompleted = true;

                // 제출된 데이터로 폼 유지 (단순히 폼 데이터만 유지)
                // 페이지는 그대로 유지 (main으로 이동하지 않음)
            } else {
                throw new Error(response.message || '승격 요청에 실패했습니다.');
            }
        } catch (error) {
            console.error('승격 요청 실패:', error);
            this.handleSubmissionError(error);
        } finally {
            this.ui.toggleLoading(false);
        }
    }

    /**
     * 제출 데이터 검증
     */
    validateSubmissionData(requestData, file) {
        if (!requestData.requestedRole) {
            if (this.ui) {
                this.ui.showAlert('요청할 역할을 선택해주세요.', 'error');
            } else {
                this.showSimpleAlert('요청할 역할을 선택해주세요.');
            }
            return false;
        }

        return true;
    }

    /**
     * 제출 에러 처리
     */
    handleSubmissionError(error) {
        let errorMessage = error.message || '승격 요청 중 오류가 발생했습니다.';

        if (error.message.includes('이미 제출된')) {
            errorMessage = '이미 제출된 승격 요청이 있습니다. 관리자 검토를 기다려주세요.';

            if (this.ui && this.ui.elements && this.ui.elements.submitBtn) {
                this.ui.elements.submitBtn.disabled = true;
                const btnText = this.ui.elements.submitBtn.querySelector('.btn-text');
                if (btnText) {
                    btnText.textContent = '처리 대기 중';
                }
            }
        } else if (error.message.includes('권한')) {
            errorMessage = '해당 역할로 승격할 권한이 없습니다.';
        } else if (error.message.includes('로그인')) {
            errorMessage = '로그인이 필요합니다. 로그인 페이지로 이동합니다.';
            setTimeout(() => {
                window.location.href = '/templates/pages/auth/login.html';
            }, 2000);
        }

        if (this.ui) {
            this.ui.showAlert(errorMessage, 'error');
        } else {
            this.showSimpleAlert(errorMessage);
        }
    }

    /**
     * 단순 알림창 표시 (UI가 없을 때 사용)
     */
    showSimpleAlert(message) {
        alert(message);
    }
}