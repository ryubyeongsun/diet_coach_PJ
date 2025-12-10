import api from './http';

export async function createUser(payload) {
  const res = await api.post('/users', payload);
  return res.data.data;
}

export async function fetchUserProfile(userId) {
  const res = await api.get(`/users/${userId}`);
  return res.data.data;
}

export async function fetchUserTdee(userId) {				
  const res = await api.get(`/users/${userId}/tdee`);
  return res.data.data;
}
