import http from "./http";

export async function signup(payload) {
  const res = await http.post("/auth/signup", payload);
  return res;
}

export async function login(payload) {
  const res = await http.post("/auth/login", payload);
  return res;
}

export async function fetchMe() {
  const res = await http.get("/auth/me");
  return res;
}
