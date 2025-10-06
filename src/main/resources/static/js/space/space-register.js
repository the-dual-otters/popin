class SpaceRegisterManager {
    async initialize() {
        const form = document.getElementById("space-register-form");
        if (form) form.addEventListener("submit", (e) => this.handleSubmit(e));

        document.querySelectorAll('[data-act="back"], [data-act="list"]').forEach((btn) => {
            btn.addEventListener("click", () => this.goList());
        });

        this.setupDateValidation();
        this.setupAddressSearch();
    }

    setupDateValidation() {
        const startDateInput = document.getElementById("startDate");
        const endDateInput = document.getElementById("endDate");

        startDateInput?.addEventListener("change", (e) => {
            if (e.target.value) {
                endDateInput.min = e.target.value;
                if (endDateInput.value && endDateInput.value < e.target.value) {
                    endDateInput.value = e.target.value;
                }
            }
        });

        const today = new Date().toISOString().split("T")[0];
        if (startDateInput) startDateInput.min = today;
    }

    setupAddressSearch() {
        const btn = document.getElementById("btn-search-address");
        btn?.addEventListener("click", function () {
            new daum.Postcode({
                oncomplete: function (data) {
                    document.getElementById("roadAddress").value = data.roadAddress;
                    document.getElementById("jibunAddress").value = data.jibunAddress;
                    document.getElementById("detailAddress")?.focus();

                    // --- 좌표 변환 추가 ---
                    const geocoder = new kakao.maps.services.Geocoder();
                    geocoder.addressSearch(data.roadAddress, function (result, status) {
                        console.log("Geocoder 호출 결과:", status, result);

                        if (status === kakao.maps.services.Status.OK) {
                            const lat = result[0].y;
                            const lng = result[0].x;
                            document.getElementById("latitude").value = lat;
                            document.getElementById("longitude").value = lng;
                            console.log("위도/경도 세팅 완료:", lat, lng);
                        } else {
                            console.warn("좌표 변환 실패:", status);
                        }
                    });
                },
            }).open();
        });
    }

    setupAddressAutoGeocode() {
        const roadAddressInput = document.getElementById("roadAddress");
        if (!roadAddressInput) return;

        const geocoder = new kakao.maps.services.Geocoder();

        roadAddressInput.addEventListener("change", () => {
            const address = roadAddressInput.value;
            if (!address || !address.trim()) return;

            geocoder.addressSearch(address, function (result, status) {
                if (status === kakao.maps.services.Status.OK) {
                    document.getElementById("latitude").value = result[0].y;
                    document.getElementById("longitude").value = result[0].x;
                    console.log("도로명 주소 변경 → 위도/경도 자동 세팅:", result[0].y, result[0].x);
                }
            });
        });
    }


    async handleSubmit(e) {
        e.preventDefault();

        const submitBtn = e.submitter || document.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        try {
            if (!this.validateForm()) return;

            // form 자체를 FormData로 변환 (hidden 필드 포함됨)
            const fd = new FormData(document.getElementById("space-register-form"));

            await apiService.createSpace(fd);
            alert("공간이 성공적으로 등록되었습니다.");
            this.goList();
        } catch (err) {
            console.error("공간 등록 실패:", err);
            this.handleError(err);
        } finally {
            if (submitBtn) submitBtn.disabled = false;
        }
    }

    validateForm() {
        const requiredFields = [
            { id: "roadAddress", name: "주소" },
            { id: "title", name: "제목" },
            { id: "areaSize", name: "면적" },
            { id: "startDate", name: "임대 시작일" },
            { id: "endDate", name: "임대 종료일" },
            { id: "rentalFee", name: "임대료" },
            { id: "contactPhone", name: "연락처" },
        ];

        for (const field of requiredFields) {
            const el = document.getElementById(field.id);
            if (!el?.value?.trim()) {
                alert(`${field.name}을(를) 입력해주세요.`);
                el?.focus();
                return false;
            }
        }
        return true;
    }

    handleError(err) {
        const msg = String(err?.message || "");

        if (msg.includes("401")) {
            alert("로그인이 필요합니다.");
        } else if (msg.includes("400") || msg.includes("422")) {
            if (err.response?.data?.errors) {
                const errors = err.response.data.errors;
                const errorMsg = Object.values(errors).join("\n");
                alert("입력 정보를 확인해주세요:\n" + errorMsg);
            } else {
                alert("입력 정보를 확인해주세요.");
            }
        } else {
            alert("등록 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    goList() {
        Pages.spaceList();
    }
}

window.SpaceRegisterManager = SpaceRegisterManager;
