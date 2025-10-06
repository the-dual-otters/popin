const ChatPage = {
    stompClient: null,
    reservationId: null,
    userId: null,
    userNickname: null,

    async init() {
        const urlParams = new URLSearchParams(window.location.search);
        const tokenParam = urlParams.get('token');

        if (tokenParam) {
            localStorage.setItem('accessToken', tokenParam);
            localStorage.setItem('authToken', tokenParam);

            const newUrl = window.location.pathname;
            window.history.replaceState({}, '', newUrl);
        }

        this.reservationId = this.getReservationIdFromUrl();
        if (!this.reservationId) {
            alert("잘못된 접근입니다.");
            window.history.back();
            return;
        }

        this.cacheElements();
        this.bindEvents();

        await this.loadCurrentUser();
        await this.loadChatContext(); // 컨텍스트 정보 로드 추가
        await this.loadMessages();
        this.connectWebSocket();
    },

    cacheElements() {
        this.el = {
            messages: document.getElementById("chat-messages"),
            input: document.getElementById("chat-input"),
            sendBtn: document.getElementById("btn-send"),
            backBtn: document.getElementById("btn-back")
        };
    },

    bindEvents() {
        this.el.sendBtn.addEventListener("click", () => this.sendMessage());
        this.el.input.addEventListener("keypress", e => {
            if (e.key === "Enter") this.sendMessage();
        });
        this.el.backBtn.addEventListener("click", () => window.history.back());
    },

    getReservationIdFromUrl() {
        const path = window.location.pathname;
        const parts = path.split("/");
        return parts.length > 2 ? parts[2] : null; // /chat/{id}
    },

    async loadCurrentUser() {
        try {
            const me = await apiService.get("/users/me");
            this.userId = me.id;
            this.userNickname = me.nickname || me.name || me.email?.split('@')[0] || '익명';
        } catch (err) {
            console.error("사용자 정보 로드 실패:", err);
            alert("로그인 정보가 필요합니다.");
            window.location.href = "/auth/login";
        }
    },

    // 채팅 컨텍스트 정보 로드
    async loadChatContext() {
        try {
            const context = await apiService.getChatContext(this.reservationId);
            this.updateChatHeader(context);
        } catch (err) {
            console.error("채팅 컨텍스트 로드 실패:", err);
            // 실패해도 기본 제목 유지
        }
    },

    // 채팅 헤더 업데이트
    updateChatHeader(context) {
        const headerTitle = document.querySelector('.chat-header h3');
        if (!headerTitle) return;

        let title = "예약 채팅";
        let subtitle = "";

        // 우선순위: 브랜드명 + 팝업제목 > 공간명
        if (context.brandName && context.popupTitle) {
            title = `${context.brandName}`;
            subtitle = context.popupTitle;
        } else if (context.popupTitle && context.spaceName) {
            title = context.popupTitle;
            subtitle = `공간: ${context.spaceName}`;
        } else if (context.spaceName) {
            title = `공간: ${context.spaceName}`;
            if (context.spaceAddress) {
                subtitle = context.spaceAddress;
            }
        } else if (context.popupTitle) {
            title = context.popupTitle;
        }

        // 예약 상태 추가
        const statusText = this.getStatusText(context.status);
        if (statusText) {
            subtitle = subtitle ? `${subtitle} • ${statusText}` : statusText;
        }

        // 헤더 HTML 업데이트
        headerTitle.innerHTML = `
            <div style="text-align: center; line-height: 1.3;">
                <div style="font-size: 16px; font-weight: 600; margin-bottom: 2px;">${title}</div>
                ${subtitle ? `<div style="font-size: 12px; opacity: 0.8;">${subtitle}</div>` : ''}
            </div>
        `;
    },

    // 예약 상태 텍스트 변환
    getStatusText(status) {
        const statusMap = {
            'PENDING': '승인 대기',
            'ACCEPTED': '승인됨',
            'REJECTED': '거절됨',
            'CANCELLED': '취소됨'
        };
        return statusMap[status] || '';
    },

    async loadMessages() {
        try {
            const messages = await apiService.getChatMessages(this.reservationId);

            messages.forEach(m => {
                this.addMessage(m.senderId, m.content, m.sentAt);
            });
            this.scrollToBottom();
        } catch (err) {
            console.error("메시지 불러오기 실패:", err);
        }
    },

    connectWebSocket() {
        const socket = new SockJS("/ws");
        this.stompClient = Stomp.over(socket);

        const token = this.getJwtToken();
        const headers = {};
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        this.stompClient.connect(
            headers,
            () => {
                console.log('WebSocket 연결 성공');
                this.stompClient.subscribe(
                    `/topic/reservation/${this.reservationId}`,
                    (msg) => {
                        const payload = JSON.parse(msg.body);
                        if (payload.error) {
                            alert(payload.error);
                            this.el.input.disabled = true;
                            this.el.sendBtn.disabled = true;
                        } else {
                            this.addMessage(
                                payload.senderId,
                                payload.content,
                                payload.sentAt
                            );
                            this.scrollToBottom();
                        }
                    }
                );
            },
            (error) => {
                console.error('WebSocket 연결 실패:', error);
                setTimeout(() => {
                    console.log('WebSocket 재연결 시도...');
                    this.connectWebSocket();
                }, 2000);
            }
        );

        this.stompClient.onclose = () => {
            console.log('WebSocket 연결이 끊어졌습니다.');
        };
    },

    getJwtToken() {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'jwtToken') {
                return value;
            }
        }
        return (
            localStorage.getItem('accessToken') ||
            localStorage.getItem('authToken') ||
            sessionStorage.getItem('accessToken') ||
            sessionStorage.getItem('authToken')
        );
    },

    sendMessage() {
        const content = this.el.input.value.trim();
        if (!content) return;

        const dto = {
            reservationId: this.reservationId,
            senderId: this.userId,
            content: content,
            sentAt: new Date().toISOString()
        };

        console.log("전송:", dto);
        this.stompClient.send("/app/chat.send", {}, JSON.stringify(dto));
        this.el.input.value = "";
    },

    // 닉네임 제거 - 1대1 채팅이므로 메시지 내용과 시간만 표시
    addMessage(senderId, content, sentAt) {
        const div = document.createElement("div");
        div.className = senderId === this.userId ? "chat-message me" : "chat-message";

        const timeText = sentAt ? new Date(sentAt).toLocaleTimeString() : "";

        div.innerHTML = `
            <div class="content">${content}</div>
            <div class="time">${timeText}</div>
        `;

        this.el.messages.appendChild(div);
        console.log("메시지 추가 완료:", content);
    },

    scrollToBottom() {
        if (this.el.messages) {
            this.el.messages.scrollTop = this.el.messages.scrollHeight;
        }
    }
};

window.ChatPage = ChatPage;