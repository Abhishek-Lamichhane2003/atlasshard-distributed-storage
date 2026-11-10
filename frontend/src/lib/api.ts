import type { AuditEvent, FileItem, NodeInfo, Overview, ShareItem, UploadSession } from '../types';

export const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

async function req(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  });
  if (!response.ok) throw new Error(await response.text());
  return response;
}

export const api = {
  async login(email: string, password: string): Promise<{email: string}> {
    return (await req('/api/auth/login', {method: 'POST', body: JSON.stringify({email, password})})).json();
  },
  async register(email: string, password: string): Promise<{email: string}> {
    return (await req('/api/auth/register', {method: 'POST', body: JSON.stringify({email, password})})).json();
  },
  async me(): Promise<{email: string}> {
    return (await req('/api/auth/me')).json();
  },
  async logout(): Promise<void> {
    await req('/api/auth/logout', {method: 'POST'});
  },
  async files(): Promise<FileItem[]> {
    return (await req('/api/files')).json();
  },
  async nodes(): Promise<NodeInfo[]> {
    return (await req('/api/nodes')).json();
  },
  async overview(): Promise<Overview> {
    return (await req('/api/system/overview')).json();
  },
  async activity(): Promise<AuditEvent[]> {
    return (await req('/api/system/activity')).json();
  },
  async toggleNode(id: string): Promise<NodeInfo> {
    return (await req(`/api/nodes/${id}/toggle`, {method: 'POST'})).json();
  },
  async placement(hash: string): Promise<NodeInfo[]> {
    return (await req(`/api/nodes/placement/${encodeURIComponent(hash)}`)).json();
  },
  async createUpload(fileName: string, logicalPath: string, totalChunks: number): Promise<UploadSession> {
    return (await req('/api/uploads', {
      method: 'POST',
      body: JSON.stringify({fileName, logicalPath, totalChunks}),
    })).json();
  },
  async uploadSession(id: string): Promise<UploadSession> {
    return (await req(`/api/uploads/${id}`)).json();
  },
  async uploadChunk(id: string, index: number, blob: Blob): Promise<UploadSession> {
    const form = new FormData();
    form.append('chunk', blob);
    return (await req(`/api/uploads/${id}/chunks/${index}`, {method: 'PUT', body: form})).json();
  },
  async completeUpload(id: string): Promise<FileItem> {
    return (await req(`/api/uploads/${id}/complete`, {method: 'POST'})).json();
  },
  async deleteFile(id: string): Promise<void> {
    await req(`/api/files/${id}`, {method: 'DELETE'});
  },
  async downloadFile(file: FileItem): Promise<void> {
    const anchor = document.createElement('a');
    anchor.href = `${API_BASE}/api/files/${file.id}/download`;
    anchor.download = file.fileName;
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  },
  async createShare(fileId: string, hours = 24): Promise<{token: string; path: string; expiresAt: string}> {
    return (await req('/api/shares', {
      method: 'POST',
      body: JSON.stringify({fileId, hours}),
    })).json();
  },
  async shares(): Promise<ShareItem[]> {
    return (await req('/api/shares')).json();
  },
  async revokeShare(id: string): Promise<void> {
    await req(`/api/shares/${id}`, {method: 'DELETE'});
  },
};
