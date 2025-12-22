// src/api/http.js
import axios from "axios";
import { getToken, clearAuth } from "../utils/auth";
import { setLoading } from "../utils/globalState";

const http = axios.create({
  // ✅ Vite proxy(/api -> 백엔드) 쓰는 구조면 이게 정답
  baseURL: "/api",
  timeout: 15000, // 5초는 식단생성/조회에 짧을 수 있어 여유줌
});

// ✅ 요청 로딩 카운터(동시요청에서도 깜빡임 방지)
let pendingCount = 0;
const startLoading = () => {
  pendingCount += 1;
  setLoading(true);
};
const endLoading = () => {
  pendingCount = Math.max(0, pendingCount - 1);
  if (pendingCount === 0) setLoading(false);
};

// Request interceptor
http.interceptors.request.use(
  (config) => {
    startLoading();

    const token = getToken();
    if (token) {
      // ✅ 표준 Bearer 헤더 고정
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    // ✅ JSON 기본값 (파일 업로드 등은 호출부에서 override 가능)
    config.headers = config.headers || {};
    if (!config.headers["Content-Type"]) {
      config.headers["Content-Type"] = "application/json";
    }

    return config;
  },
  (error) => {
    endLoading();
    return Promise.reject(error);
  },
);
																																													
// Response interceptor
http.interceptors.response.use(
  (response) => {
    endLoading();

    // ✅ 우리 ApiResponse<T> 패턴: { success, message, data }
    const body = response?.data;

    // 백엔드에서 success:false면 에러로 처리
    if (body && body.success === false) {
      const msg = body.message || "오류가 발생했습니다.";
      const err = new Error(msg);
      err.payload = body;
      return Promise.reject(err);
    }

    // ✅ 기본은 body를 그대로 반환(호출부에서 data만 쓰는 것도 가능)
    return body;
  },
  (error) => {
    endLoading();

    // ✅ axios cancel / abort는 조용히 넘길 수도 있음 (원하면 주석 해제)
    // if (axios.isCancel(error) || error.code === "ERR_CANCELED") {
    //   return Promise.reject(error);
    // }

    if (error.response) {
      const { status, data } = error.response;

      if (status === 401) {
        clearAuth();
        alert("로그인 시간이 만료되었습니다.\n다시 로그인해 주세요.");
        window.location.href = "/login";
      } else if (status >= 500) {
        alert("서버 오류가 발생했습니다.\n잠시 후 다시 시도해 주세요.");
      } else {
        // ✅ 400/403/404 등: 백엔드 message가 있으면 우선 노출
        const msg =
          (data && (data.message || data.error || data.msg)) ||
          "요청 처리 중 오류가 발생했습니다.";

        // 특정 에러 메시지는 alert 띄우지 않음 (페이지에서 별도 처리)
        if (msg !== "해당 사용자의 최근 식단 플랜이 없습니다.") {
          alert(msg);
        }
      }
    } else {
      // 네트워크 에러 등 response가 없는 경우
      alert("네트워크 오류가 발생했습니다.\n연결을 확인해 주세요.");
    }

    return Promise.reject(error);
  },
);

export default http;
