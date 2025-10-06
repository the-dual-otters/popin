(function () {
    // 리워드 룰렛 UI 열기
    async function openRewardRoulette(missionSetId) {
        // 이미 발급 여부 확인
        try {
            const myReward = await apiService.get(`/rewards/my/${encodeURIComponent(missionSetId)}`);
            if (myReward && myReward.status) {
                alert(`이미 리워드(${myReward.optionName})를 받으셨습니다.`);
                return;
            }
        } catch (e) {
            console.warn("myReward API 없음 or 실패 -> 무시");
        }

        // 옵션 목록
        let options;
        try {
            options = await apiService.get(`/rewards/options/${encodeURIComponent(missionSetId)}`);
        } catch (e) {
            alert("리워드 옵션 조회에 실패했습니다. 잠시 후 다시 시도해주세요.");
            return;
        }
        if (!options || options.length === 0) {
            alert("리워드 옵션이 없습니다.");
            return;
        }

        // 모달
        const backdrop = document.createElement("div");
        backdrop.className = "modal-backdrop";
        backdrop.style.cssText = `
      position: fixed; inset:0;
      background: rgba(0,0,0,0.45);
      display:flex; align-items:center; justify-content:center;
      z-index:9999;
    `;
        backdrop.innerHTML = `
  <div class="modal-card" style="background:#fff; padding:20px; border-radius:30px; text-align:center; position:relative;">
    <button class="modal-close" id="close-btn">&times;</button>

    <div class="roulette-container" style="position:relative; width:300px; height:300px; margin:30px auto;">
      <canvas id="rouletteCanvas" width="300" height="300"></canvas>
      <div style="position:absolute;top:-40px;left:50%;transform:translateX(-50%);font-size:40px;color:#79f;">▼</div>
    </div>
    <div style="display:flex; gap:8px; justify-content:center; margin-top:16px;">
      <button id="spin-btn" class="submit-btn">돌리기</button>
    </div>
  </div>
`;

        document.body.appendChild(backdrop);

        const canvas = backdrop.querySelector("#rouletteCanvas");
        const ctx = canvas.getContext("2d");
        let spinning = false;
        let currentAngle = 0;

        drawWheel(ctx, options, currentAngle);

        function close() {
            document.body.removeChild(backdrop);
        }
        backdrop.querySelector("#close-btn").onclick = close;

        backdrop.querySelector("#spin-btn").onclick = function () {
            if (spinning) return;
            spinning = true;

            const spinAngle = 360 * 5 + Math.random() * 360;
            const finalAngle = currentAngle + spinAngle;

            const duration = 4000;
            const start = performance.now();

            function animate(now) {
                const elapsed = now - start;
                const progress = Math.min(elapsed / duration, 1);
                const angle = currentAngle + spinAngle * easeOut(progress);

                drawWheel(ctx, options, angle);

                if (progress < 1) {
                    requestAnimationFrame(animate);
                } else {
                    spinning = false;
                    currentAngle = finalAngle % 360;

                    // 당첨 인덱스 계산 (윗부분 화살표 기준)
                    const idx = getWinnerIndex(currentAngle, options.length);
                    const chosen = options[idx];

                    // 서버 claim
                    apiService
                        .post("/rewards/claim", {
                            missionSetId,
                            optionId: chosen.id,
                        })
                        .then(() => {
                            alert(`축하합니다! [${chosen.name}] 리워드 당첨 🎉`);
                            close();
                        })
                        .catch((err) => {
                            alert("리워드 발급 실패: " + (err?.message || err || "알 수 없는 오류"));
                            close();
                        });
                }
            }

            requestAnimationFrame(animate);
        };
    }

    // 룰렛 그리기
    function drawWheel(ctx, options, angle) {
        const w = ctx.canvas.width;
        const h = ctx.canvas.height;
        const cx = w / 2;
        const cy = h / 2;
        const r = w / 2 - 10; // 여유 공간
        const step = (2 * Math.PI) / options.length;

        ctx.clearRect(0, 0, w, h);

        const colors = ["#79f", "#e0e7ff"];

        // 섹터 그리기
        options.forEach((opt, i) => {
            const start = i * step + (angle * Math.PI) / 180;
            const end = start + step;

            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.arc(cx, cy, r, start, end);
            ctx.closePath();

            ctx.fillStyle = colors[i % colors.length];
            ctx.fill();

            ctx.strokeStyle = "#e3e3e3"; // 얇은 경계선
            ctx.lineWidth = 1;
            ctx.stroke();

            // 텍스트 (섹터 중앙 근처)
            ctx.save();
            ctx.translate(cx, cy);
            ctx.rotate(start + step / 2);
            ctx.textAlign = "center";
            ctx.fillStyle = "#fff";
            ctx.font = "bold 20px sans-serif";
            ctx.shadowColor = "rgba(147,147,147,0.6)"; // 그림자 색
            ctx.shadowBlur = 4;                  // 번짐 정도
            ctx.shadowOffsetX = 1;                // 가로 오프셋
            ctx.shadowOffsetY = 1;                // 세로 오프셋
            ctx.fillText(opt.name, r * 0.7, 5); // r*0.7 위치 (안쪽에 글씨)
            ctx.restore();
        });
        ctx.stroke();
    }




    function easeOut(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    // 현재 각도로 당첨 인덱스
    function getWinnerIndex(angle, count) {
        const step = 360 / count;
        const adjusted = (270 - angle + 360) % 360; // 위쪽 270도 기준
        return Math.floor(adjusted / step);
    }

    window.rewardClaim = openRewardRoulette;
})();
