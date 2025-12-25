# 오늘 주요 변경 사항 (발표용 요약)

## 1) 장보기 API/도메인 리팩토링
- 장보기 API를 PRD 구조로 정렬: `GET /api/meal-plans/{planId}/shopping?range=TODAY|WEEK|MONTH&date=YYYY-MM-DD`
- TODAY는 `date` 필수, WEEK/MONTH는 요약/참고용 메시지 제공
- 예산 계산은 `dailyBudget = monthlyBudget / totalDays` 기준
- 응답에 `estimatedTotal`, `warningMessage`, `purchaseRecommended` 포함

## 2) 예산 근사치 필터링 강화
- 장보기 합계가 예산을 크게 초과하지 않도록 필터링 로직 적용
- 가격/그램 기반 추정 비용으로 고비용 항목 우선 제외
- AI는 가격을 모르는 구조로 정리 (프롬프트에서 예산 정보 제거)

## 3) 11번가 상품 매핑 안정화
- 검색 정규화/필터 강화 + 후보 3개 이상일 때만 AI 리랭크
- 상품 상세 링크가 없을 경우 11번가 URL 자동 보정
- 상품 매핑이 부정확할 수 있다는 UX 안내 추가

## 4) 쇼핑 UI/UX 개선
- TODAY 날짜 선택 가능 (date picker)
- 경고 메시지, 구매 가능 여부 안내
- “11번가 구매” + “11번가 검색” 버튼 동시 제공
- 쇼핑 페이지 로딩 문구를 냉장고/장보기 콘셉트로 개선

## 5) 구매 확정/가계부 개선
- 구매확정 시 쇼핑 리스트에서 해당 항목 숨김 처리
- 장바구니/구매완료 목록을 사용자별 로컬 저장 (새로고침/재로그인 유지)
- 가계부 예산을 사용자가 입력한 월 예산으로 자동 설정 및 저장

## 6) 식단 상세 UI 정리
- 식단 상세(아침/점심/저녁)에서 High Protein 배지 제거
