// reservation-settings.js
const ReservationSettings = {
    popupId: null,

    init(popupId) {
        this.popupId = popupId;
        this.loadSettings();
        this.setupEventListeners();
    },

    async loadSettings() {
        try {
            const basic = await apiService.getPopupBasicSettings(this.popupId);

            document.getElementById("time-slot-interval").value = basic.timeSlotInterval || 30;
            document.getElementById("max-capacity").value = basic.maxCapacityPerSlot || 10;

            console.log("예약 설정 불러오기 성공:", basic);
        } catch (err) {
            console.error("예약 설정 불러오기 실패:", err);
        }
    },

    async saveSettings() {
        const settings = {
            timeSlotInterval: parseInt(document.getElementById("time-slot-interval").value),
            maxCapacityPerSlot: parseInt(document.getElementById("max-capacity").value)
        };

        try {
            await apiService.updatePopupBasicSettings(this.popupId, settings);
            alert("설정이 저장되었습니다.");
            await this.loadTimeSlots();
        } catch (err) {
            console.error("설정 저장 실패:", err);
            alert("설정 저장에 실패했습니다.");
        }
    },

    async loadTimeSlots() {
        const date = document.getElementById("selected-date").value;
        if (!date) return;

        try {
            const slots = await apiService.getAvailableSlotsWithCapacity(this.popupId, date);
            this.renderTimeSlots(slots);
        } catch (err) {
            console.error("시간대 로딩 실패:", err);
            alert("시간대 정보를 불러올 수 없습니다.");
        }
    },

    renderTimeSlots(slots) {
        const container = document.getElementById("time-slots-container");
        container.innerHTML = "";

        if (!slots || slots.length === 0) {
            container.innerHTML = "<div class='empty'>예약 가능한 시간이 없습니다.</div>";
            return;
        }

        slots.forEach(slot => {
            const div = document.createElement("div");
            div.className = "time-slot";
            div.textContent = `${slot.startTime} ~ ${slot.endTime} (남은 자리: ${slot.remainingCapacity})`;
            container.appendChild(div);
        });
    },

    setupEventListeners() {
        const saveBtn = document.querySelector(".save-settings-btn");
        if (saveBtn) {
            saveBtn.addEventListener("click", () => this.saveSettings());
        }

        const dateInput = document.getElementById("selected-date");
        if (dateInput) {
            dateInput.addEventListener("change", () => this.loadTimeSlots());
        }
    }
};

window.ReservationSettings = ReservationSettings;
