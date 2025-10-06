// 결제 관리자 클래스
class PaymentManager {
    constructor(config) {
        this.reservationId = config.reservationId;
        this.amount = config.amount;
        this.itemName = config.itemName;
        this.onSuccess = config.onSuccess || this.defaultSuccessHandler;
        this.onError = config.onError || this.defaultErrorHandler;
        this.onCancel = config.onCancel || this.defaultCancelHandler;

        this.paymentProcessed = false;
        this.paymentWindow = null;
        this.checkInterval = null;
    }

    // ===== 카카오페이 결제 =====
    async processKakaoPay() {
        try {
            this.disablePaymentButtons();
            this.paymentProcessed = false;

            // 카카오페이 결제 준비 API 호출
            const response = await fetch('/api/popup-reservations/payment/ready', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${apiService.getStoredToken()}`
                },
                body: JSON.stringify({
                    reservationId: this.reservationId,
                    paymentMethod: 'kakao'
                }),
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error('카카오페이 결제 준비 실패');
            }

            const result = await response.json();

            if (result.success) {
                this.openKakaoPayWindow(result.redirectUrl);
            } else {
                throw new Error(result.message || '카카오페이 결제 준비 실패');
            }

        } catch (error) {
            console.error('카카오페이 결제 오류:', error);
            this.enablePaymentButtons();
            this.onError('카카오페이 결제 중 오류가 발생했습니다: ' + error.message);
        }
    }

    // 카카오페이 결제창 열기
    openKakaoPayWindow(paymentUrl) {
        this.paymentWindow = window.open(
            paymentUrl,
            'kakaoPayment',
            'width=500,height=600,scrollbars=yes'
        );

        // 결제창이 닫히는지 주기적으로 체크
        this.checkInterval = setInterval(() => {
            if (this.paymentWindow.closed) {
                clearInterval(this.checkInterval);
                // 결제창이 닫혀도 성공/실패 콜백이 없으면 취소로 간주
                setTimeout(() => {
                    if (!this.paymentProcessed) {
                        this.enablePaymentButtons();
                        this.onCancel('결제가 취소되었습니다.');
                    }
                }, 1000);
            }
        }, 1000);
    }

    // ===== 네이버페이 결제 (준비중으로 처리) =====
    async processNaverPay() {
        alert("네이버페이 결제는 준비 중입니다.")
        // try {
        //     this.disablePaymentButtons();
        //     this.paymentProcessed = false;
        //
        //     // 네이버페이 결제 준비 API 호출
        //     const response = await fetch('/api/popup-reservations/payment/ready', {
        //         method: 'POST',
        //         headers: {
        //             'Content-Type': 'application/json',
        //             'Authorization': `Bearer ${apiService.getStoredToken()}`
        //         },
        //         body: JSON.stringify({
        //             reservationId: this.reservationId,
        //             paymentMethod: 'naver'
        //         }),
        //         credentials: 'include'
        //     });
        //
        //     if (!response.ok) {
        //         throw new Error('네이버페이 결제 준비 실패');
        //     }
        //
        //     const result = await response.json();
        //
        //     if (result.success) {
        //         this.openNaverPayWindow(result.redirectUrl);
        //     } else {
        //         throw new Error(result.message || '네이버페이 결제 준비 실패');
        //     }
        //
        // } catch (error) {
        //     console.error('네이버페이 결제 오류:', error);
        //     this.enablePaymentButtons();
        //     this.onError('네이버페이 결제 중 오류가 발생했습니다: ' + error.message);
        // }
    }

    // 네이버페이 결제창 열기
    openNaverPayWindow(paymentUrl) {
        this.paymentWindow = window.open(
            paymentUrl,
            'naverPayment',
            'width=500,height=600,scrollbars=yes'
        );

        // 결제창이 닫히는지 주기적으로 체크
        this.checkInterval = setInterval(() => {
            if (this.paymentWindow.closed) {
                clearInterval(this.checkInterval);
                setTimeout(() => {
                    if (!this.paymentProcessed) {
                        this.enablePaymentButtons();
                        this.onCancel('결제가 취소되었습니다.');
                    }
                }, 1000);
            }
        }, 1000);
    }

    // ===== 결제 성공/실패 처리 =====
    handleSuccess(reservationId) {
        this.paymentProcessed = true;
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
        }

        console.log('결제 성공:', reservationId);
        this.onSuccess(reservationId);
    }

    handleError(errorMessage) {
        this.paymentProcessed = true;
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
        }

        console.error('결제 실패:', errorMessage);
        this.enablePaymentButtons();
        this.onError(errorMessage);
    }

    // ===== 예약 취소 =====
    async cancelReservation() {
        if (!confirm('결제를 취소하시겠습니까?\n예약도 함께 취소됩니다.')) {
            return false;
        }

        try {
            await fetch(`/api/reservations/${this.reservationId}/cancel`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${apiService.getStoredToken()}`
                },
                credentials: 'include'
            });

            return true;
        } catch (error) {
            console.error('예약 취소 실패:', error);
            this.onError('예약 취소 중 오류가 발생했습니다.');
            return false;
        }
    }

    // ===== UI 관리 =====
    disablePaymentButtons() {
        document.querySelectorAll('.payment-btn').forEach(btn => {
            btn.disabled = true;
            btn.style.opacity = '0.6';
        });
    }

    enablePaymentButtons() {
        document.querySelectorAll('.payment-btn').forEach(btn => {
            btn.disabled = false;
            btn.style.opacity = '1';
        });
    }

    // ===== 기본 핸들러들 =====
    defaultSuccessHandler(reservationId) {
        alert('결제가 완료되었습니다!');
    }

    defaultErrorHandler(errorMessage) {
        alert('결제에 실패했습니다: ' + errorMessage);
    }

    defaultCancelHandler(message) {
        alert(message || '결제가 취소되었습니다.');
    }

    cleanup() {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
        }
        if (this.paymentWindow && !this.paymentWindow.closed) {
            this.paymentWindow.close();
        }
    }
}

// ===== 결제 UI 생성기 =====
class PaymentUIBuilder {
    static createPaymentSection(config) {
        const {
            reservationData,
            popupData,
            selectedDate,
            selectedTimeSlot,
            partySize,
            customerName
        } = config;

        const totalAmount = popupData.entryFee * partySize;
        const reservationId = reservationData.id || reservationData.reservationId;

        return `
            <section class="block" id="payment-section">
                <div class="page-header">
                    <h2>결제하기</h2>
                    <p class="subtitle">예약이 완료되었습니다. 결제를 진행해주세요.</p>
                </div>

                <!-- 예약 정보 요약 -->
                <div class="payment-summary">
                    <h3>예약 정보</h3>
                    <div class="summary-item">
                        <span>팝업명</span>
                        <span>${popupData.title}</span>
                    </div>
                    <div class="summary-item">
                        <span>예약자</span>
                        <span>${customerName}</span>
                    </div>
                    <div class="summary-item">
                        <span>예약일시</span>
                        <span>${selectedDate} ${selectedTimeSlot.startTime}</span>
                    </div>
                    <div class="summary-item">
                        <span>인원</span>
                        <span>${partySize}명</span>
                    </div>
                    <div class="summary-item total">
                        <span>결제금액</span>
                        <span class="amount">${totalAmount.toLocaleString()}원</span>
                    </div>
                </div>

                <!-- 결제 방법 선택 -->
                <div class="payment-methods">
                    <h3>결제 방법 선택</h3>
                    <div class="payment-options">
                        <button class="payment-btn" data-method="kakao" 
                                onclick="window.currentPaymentManager.processKakaoPay()">
                            <img src="/images/kakao-pay-logo.png" alt="카카오페이" onerror="this.style.display='none'">
                            <span>카카오페이</span>
                        </button>
                        <button class="payment-btn" data-method="naver" 
                                onclick="window.currentPaymentManager.processNaverPay()">
                            <img src="/images/naver-pay-logo.png" alt="네이버페이" onerror="this.style.display='none'">
                            <span>네이버페이</span>
                        </button>
                    </div>
                </div>

                <!-- 취소 버튼 -->
                <div class="payment-actions">
                    <button class="btn-secondary" onclick="window.currentPaymentManager.cancelReservation().then(success => { if(success) window.location.reload(); })">
                        결제 취소 (예약도 취소됨)
                    </button>
                </div>
            </section>
        `;
    }
}

// ===== 전역 콜백 함수들  =====
window.handlePaymentSuccess = function(reservationId) {
    if (window.currentPaymentManager) {
        window.currentPaymentManager.handleSuccess(reservationId);
    }
};

window.handlePaymentError = function(errorMessage) {
    if (window.currentPaymentManager) {
        window.currentPaymentManager.handleError(errorMessage);
    }
};

window.PaymentManager = PaymentManager;
window.PaymentUIBuilder = PaymentUIBuilder;