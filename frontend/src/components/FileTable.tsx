import { useState } from 'react';
import { Download, File, Link2, ShieldCheck, Trash2 } from 'lucide-react';
import { API_BASE, api } from '../lib/api';
import type { FileItem } from '../types';

const size = (n:number) => n >= 1e9 ? `${(n/1e9).toFixed(1)} GB` : n >= 1e6 ? `${(n/1e6).toFixed(1)} MB` : `${Math.max(1,n/1e3).toFixed(0)} KB`;

export default function FileTable({files,onShareCreated,onDeleted}:{files: FileItem[]; onShareCreated?:()=>void; onDeleted?:()=>void}) {
  const [message, setMessage] = useState('');

  async function share(file: FileItem) {
    try {
      const result = await api.createShare(file.id, 24);
      const url = new URL(`${API_BASE}${result.path}`, window.location.origin).toString();
      await navigator.clipboard.writeText(url);
      setMessage(`24-hour link copied for ${file.fileName}`);
      onShareCreated?.();
      window.setTimeout(() => setMessage(''), 3500);
    } catch {
      setMessage('Unable to create a share link.');
    }
  }

  async function remove(file: FileItem) {
    if (!window.confirm(`Delete ${file.fileName} (v${file.versionNumber})?`)) return;

    try {
      await api.deleteFile(file.id);
      setMessage(`Deleted ${file.fileName} v${file.versionNumber}`);
      onDeleted?.();
      window.setTimeout(() => setMessage(''), 3500);
    } catch {
      setMessage('Unable to delete this file version.');
    }
  }

  if (!files.length) return <div className="table-empty">No stored files yet.</div>;

  return <>
    {message && <div className="table-toast">{message}</div>}
    <div className="file-table">
      <div className="file-head"><span>Name</span><span>Size</span><span>Version</span><span>Integrity</span><span>Actions</span></div>
      {files.map(file => <div className="file-row" key={file.id}>
        <span className="file-name"><File size={18}/><span><b>{file.fileName}</b><small>{file.logicalPath}</small></span></span>
        <span>{size(file.sizeBytes)}</span>
        <span>v{file.versionNumber}</span>
        <span className="integrity"><ShieldCheck size={15}/> SHA-256</span>
        <span className="row-actions">
          <button title="Download" onClick={() => api.downloadFile(file)}><Download size={16}/></button>
          <button title="Create 24-hour share link" onClick={() => share(file)}><Link2 size={16}/></button>
          <button className="danger" title="Delete this version" onClick={() => remove(file)}><Trash2 size={16}/></button>
        </span>
      </div>)}
    </div>
  </>;
}
