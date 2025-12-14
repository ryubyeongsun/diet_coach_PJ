import api from './http';

export async function signup(payload) {
  const res = await api.post('/auth/signup', payload);
  return res.data;
}

export async function login(payload) {
  const res = await api.post('/auth/login', payload);
  return res.data;
}

export async function fetchMe() {
  const res = await api.get('/auth/me');
  return res.data;
}
