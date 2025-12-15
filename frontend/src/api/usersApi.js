import http from './http';

/**
 * 사용자의 프로필 정보를 업데이트합니다.
 * @param {number} userId - 사용자 ID
 * @param {object} profileData - 업데이트할 프로필 데이터
 * @returns {Promise<object>} - 업데이트된 프로필 정보
 */
export function updateUserProfile(userId, profileData) {
    return http.put(`/users/${userId}/profile`, profileData);
}

/**
 * 특정 사용자의 프로필 정보를 조회합니다.
 * @param {number} userId - 사용자 ID
 * @returns {Promise<object>} - 사용자 프로필 정보
 */
export function fetchUserProfile(userId) {
    return http.get(`/users/${userId}`);
}