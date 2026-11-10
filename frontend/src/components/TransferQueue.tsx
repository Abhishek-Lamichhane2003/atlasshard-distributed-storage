import { ArrowUp, CheckCircle2, CircleAlert, ShieldCheck } from 'lucide-react';
import type { TransferItem } from '../types';

export default function TransferQueue({items}:{items: TransferItem[]}) {
  if (!items.length) {
    return <div className="queue-empty">No active transfers. Upload a file to see multipart progress.</div>;
  }
  return <div className="transfer-list">
    {items.map(item => <div className="transfer" key={item.id}>
      <div className={`transfer-icon ${item.status}`}>
        {item.status === 'complete' ? <CheckCircle2/> : item.status === 'failed' ? <CircleAlert/> : item.status === 'verifying' ? <ShieldCheck/> : <ArrowUp/>}
      </div>
      <div>
        <b>{item.name}</b>
        <span>{item.status === 'verifying' ? 'SHA-256 verification' : item.status}</span>
        <div className="progress"><i style={{width: `${item.progress}%`}} /></div>
      </div>
      <em>{Math.round(item.progress)}%</em>
    </div>)}
  </div>;
}
