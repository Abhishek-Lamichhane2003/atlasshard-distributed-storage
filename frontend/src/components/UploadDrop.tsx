import { useRef, useState } from 'react';
import { UploadCloud } from 'lucide-react';
import { api } from '../lib/api';
import type { TransferItem, UploadSession } from '../types';

const CHUNK_SIZE = 4 * 1024 * 1024;
const CONCURRENCY = 4;

export default function UploadDrop({
  onDone,
  onTransfer,
}:{
  onDone:()=>void;
  onTransfer:(item: TransferItem)=>void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const [status, setStatus] = useState('');

  async function upload(file: File) {
    const total = Math.max(1, Math.ceil(file.size / CHUNK_SIZE));
    const storageKey = `atlasshard-upload:${file.name}:${file.size}:${file.lastModified}`;
    const transferId = `${file.name}:${file.lastModified}`;
    let lastProgress = 0;
    try {
      setStatus('Preparing multipart session...');
      let session: UploadSession | null = null;
      const previousId = localStorage.getItem(storageKey);
      if (previousId) {
        try {
          const previous = await api.uploadSession(previousId);
          if (previous.fileName === file.name && previous.totalChunks === total) session = previous;
        } catch {
          localStorage.removeItem(storageKey);
        }
      }
      if (!session) {
        session = await api.createUpload(file.name, `/${file.name}`, total);
        localStorage.setItem(storageKey, session.id);
      }

      const received = new Set(session.received);
      let completed = received.size;
      lastProgress = (completed / total) * 100;
      onTransfer({id: transferId, name: file.name, progress: lastProgress, status: 'uploading'});
      const pending = Array.from({length: total}, (_, index) => index).filter(index => !received.has(index));
      let cursor = 0;

      async function worker() {
        while (cursor < pending.length) {
          const position = cursor++;
          const index = pending[position];
          const start = index * CHUNK_SIZE;
          const end = Math.min(file.size, start + CHUNK_SIZE);
          await api.uploadChunk(session!.id, index, file.slice(start, end));
          completed += 1;
          const progress = (completed / total) * 100;
          lastProgress = progress;
          setStatus(`Uploading ${completed}/${total} chunks`);
          onTransfer({id: transferId, name: file.name, progress, status: 'uploading'});
        }
      }

      await Promise.all(Array.from({length: Math.min(CONCURRENCY, pending.length || 1)}, () => worker()));
      setStatus('Verifying SHA-256...');
      onTransfer({id: transferId, name: file.name, progress: 100, status: 'verifying'});
      await api.completeUpload(session.id);
      localStorage.removeItem(storageKey);
      setStatus('Stored successfully');
      onTransfer({id: transferId, name: file.name, progress: 100, status: 'complete'});
      onDone();
    } catch {
      setStatus('Upload paused. Select the same file to resume.');
      onTransfer({id: transferId, name: file.name, progress: lastProgress, status: 'failed'});
    }
  }

  return <div className="dropzone" onClick={() => input.current?.click()}>
    <input ref={input} type="file" hidden onChange={e => e.target.files?.[0] && upload(e.target.files[0])} />
    <UploadCloud size={30}/>
    <b>Drop files into the storage fabric</b>
    <span>4 MiB chunks - four parallel workers - resumable sessions</span>
    {status && <small>{status}</small>}
  </div>;
}
