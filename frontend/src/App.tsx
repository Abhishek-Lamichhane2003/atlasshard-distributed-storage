import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  Boxes,
  Cloud,
  Database,
  FileStack,
  Gauge,
  HardDrive,
  HeartPulse,
  LockKeyhole,
  LogOut,
  Search,
  Share2,
  Upload,
} from 'lucide-react';
import { API_BASE, api } from './lib/api';
import type { AuditEvent, FileItem, NodeInfo, Overview, ShareItem, TransferItem } from './types';
import NodeTopology from './components/NodeTopology';
import FileTable from './components/FileTable';
import TransferQueue from './components/TransferQueue';
import UploadDrop from './components/UploadDrop';
import AuthPanel from './components/AuthPanel';

const EMPTY_OVERVIEW: Overview = {
  product: 'AtlasShard',
  nodes: 0,
  healthyNodes: 0,
  replicationFactor: 3,
  hashing: 'SHA-256 + consistent hash ring',
  fileVersions: 0,
  uniqueObjects: 0,
  logicalBytes: 0,
  physicalBytes: 0,
  dedupSavingsPct: 0,
};

const formatBytes = (bytes: number) => {
  if (bytes >= 1e12) return `${(bytes / 1e12).toFixed(1)} TB`;
  if (bytes >= 1e9) return `${(bytes / 1e9).toFixed(1)} GB`;
  if (bytes >= 1e6) return `${(bytes / 1e6).toFixed(1)} MB`;
  if (bytes >= 1e3) return `${(bytes / 1e3).toFixed(0)} KB`;
  return `${bytes} B`;
};

function App() {
  const [page, setPage] = useState('Overview');
  const [files, setFiles] = useState<FileItem[]>([]);
  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [overview, setOverview] = useState<Overview>(EMPTY_OVERVIEW);
  const [activity, setActivity] = useState<AuditEvent[]>([]);
  const [shares, setShares] = useState<ShareItem[]>([]);
  const [transfers, setTransfers] = useState<TransferItem[]>([]);
  const [user, setUser] = useState<string | null>(null);
  const [auth, setAuth] = useState(false);
  const [clock, setClock] = useState(new Date());
  const [query, setQuery] = useState('');
  const [apiRtt, setApiRtt] = useState<number | null>(null);

  async function refreshPublic() {
    const started = performance.now();
    try {
      const [nextOverview, nextNodes] = await Promise.all([api.overview(), api.nodes()]);
      setOverview(nextOverview);
      setNodes(nextNodes);
      setApiRtt(Math.max(1, Math.round(performance.now() - started)));
    } catch {
      setApiRtt(null);
    }
  }

  async function refreshPrivate() {
    if (!user) {
      setFiles([]);
      setActivity([]);
      setShares([]);
      return;
    }
    const [fileResult, activityResult, shareResult] = await Promise.allSettled([
      api.files(),
      api.activity(),
      api.shares(),
    ]);
    if (fileResult.status === 'fulfilled') setFiles(fileResult.value);
    if (activityResult.status === 'fulfilled') setActivity(activityResult.value);
    if (shareResult.status === 'fulfilled') setShares(shareResult.value);
  }

  useEffect(() => {
    refreshPublic();
    api.me().then(result => setUser(result.email)).catch(() => setUser(null));
    const clockTimer = window.setInterval(() => setClock(new Date()), 1000);
    const refreshTimer = window.setInterval(refreshPublic, 15000);
    return () => {
      window.clearInterval(clockTimer);
      window.clearInterval(refreshTimer);
    };
  }, []);

  useEffect(() => {
    refreshPrivate();
  }, [user]);

  async function toggleNode(id: string) {
    if (!user) {
      setAuth(true);
      return;
    }
    try {
      const node = await api.toggleNode(id);
      setNodes(current => current.map(item => item.id === id ? node : item));
      await refreshPublic();
      await refreshPrivate();
    } catch {
      // Keep server state authoritative if the operation is rejected.
    }
  }

  async function signOut() {
    await api.logout();
    setUser(null);
    setFiles([]);
    setActivity([]);
    setShares([]);
  }

  function updateTransfer(item: TransferItem) {
    setTransfers(current => {
      const index = current.findIndex(existing => existing.id === item.id);
      if (index < 0) return [item, ...current].slice(0, 8);
      const next = [...current];
      next[index] = item;
      return next;
    });
  }

  async function revokeShare(id: string) {
    await api.revokeShare(id);
    await refreshPrivate();

    // Kafka audit events are persisted asynchronously.
    // Refresh once more after the consumer has had time to process the event.
    window.setTimeout(() => {
      refreshPrivate();
    }, 750);
  }

  const filteredFiles = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return files;
    return files.filter(file => [file.fileName, file.logicalPath, file.contentHash]
      .some(value => value.toLowerCase().includes(needle)));
  }, [files, query]);

  const nav = [
    ['Overview', Gauge],
    ['Files', FileStack],
    ['Transfers', Upload],
    ['Storage Nodes', Boxes],
    ['Shared Links', Share2],
    ['Activity', Activity],
  ] as const;

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark"><HardDrive/></div><div><strong>ATLASSHARD</strong><span>STORAGE CONTROL PLANE</span></div></div>
      <nav>{nav.map(([name, Icon]) => <button className={page === name ? 'active' : ''} onClick={() => setPage(name)} key={name}><Icon size={19}/><span>{name}</span></button>)}</nav>
      <div className="sidebar-bottom">
        <button onClick={() => window.open(`${API_BASE}/actuator/health`, '_blank')}><HeartPulse size={18}/><span>API health</span></button>
        <div className={`cluster-status ${overview.healthyNodes === overview.nodes && overview.nodes > 0 ? '' : 'degraded'}`}><i/>{overview.healthyNodes === overview.nodes && overview.nodes > 0 ? 'cluster healthy' : 'cluster degraded'}<small>{overview.healthyNodes}/{overview.nodes} simulated nodes - RF {overview.replicationFactor}</small></div>
      </div>
    </aside>

    <main>
      <header className="topbar">
        <label className="search"><Search size={17}/><input value={query} onChange={e => setQuery(e.target.value)} onFocus={() => setPage('Files')} placeholder="Search files, paths, hashes..."/></label>
        <div className="top-actions">
          <span className="utc">{clock.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit', second:'2-digit'})}</span>
          {user ? <><span className="user-chip">{user}</span><button className="outline" onClick={signOut}><LogOut size={16}/> Sign out</button></> : <button className="outline" onClick={() => setAuth(true)}><LockKeyhole size={16}/> Sign in</button>}
          <button className="primary" onClick={() => user ? setPage('Files') : setAuth(true)}><Upload size={16}/> Upload</button>
        </div>
      </header>

      {page === 'Overview' && <>
        <section className="hero">
          <div className="hero-overlay"/>
          <div className="hero-copy">
            <span className="eyebrow">DISTRIBUTED OBJECT STORAGE</span>
            <h1>Store once.<br/><em>Survive failure.</em></h1>
            <p>Content-addressed object storage with resumable multipart transfers, versioning, deduplication, and a live replica-placement simulator.</p>
            <div className="hero-actions"><button className="primary" onClick={() => setPage('Files')}><Cloud size={17}/> Open workspace</button><button className="outline" onClick={() => setPage('Storage Nodes')}><Boxes size={17}/> View topology</button></div>
          </div>
          <div className="hero-metrics">
            <div><span>FILE VERSIONS</span><b>{overview.fileVersions.toLocaleString()}</b><small>{overview.uniqueObjects.toLocaleString()} unique objects</small></div>
            <div><span>LOGICAL DATA</span><b>{formatBytes(overview.logicalBytes)}</b><small>{formatBytes(overview.physicalBytes)} unique bytes</small></div>
            <div><span>DEDUP SAVINGS</span><b>{overview.dedupSavingsPct.toFixed(1)}%</b><small>SHA-256 content map</small></div>
          </div>
        </section>
        <section className="overview-grid">
          <div className="panel topology-panel"><div className="panel-title"><div><span>REPLICA PLACEMENT SIMULATOR</span><h3>Node topology</h3></div><button className="mini">interactive</button></div><NodeTopology nodes={nodes} onToggle={toggleNode}/><p className="hint">Select a node to simulate failure or recovery. The virtual-node hash ring rebalances placement across healthy nodes.</p></div>
          <div className="panel transfers-panel"><div className="panel-title"><div><span>BROWSER TRANSFERS</span><h3>Live queue</h3></div><span className="live-dot">SESSION</span></div><TransferQueue items={transfers}/></div>
          <div className="panel security-panel"><Database size={23}/><span>CONTENT ADDRESSING</span><h3>Store duplicate content once</h3><p>Object keys are derived from SHA-256. Independent logical versions can reference the same immutable object bytes.</p><div className="hashline">content/&lt;sha256&gt;</div></div>
        </section>
      </>}

      {page === 'Files' && <section className="workspace"><div className="workspace-head"><div><span className="eyebrow">OBJECT WORKSPACE</span><h1>Files & versions</h1><p>Manage logical paths backed by content-addressed object storage.</p></div></div>{user ? <><UploadDrop onDone={() => {refreshPublic(); refreshPrivate();}} onTransfer={updateTransfer}/><FileTable files={filteredFiles} onShareCreated={refreshPrivate} onDeleted={() => {refreshPublic(); refreshPrivate();}}/></> : <AuthRequired onOpen={() => setAuth(true)} text="Sign in to upload and manage stored files."/>}</section>}

      {page === 'Transfers' && <section className="workspace"><div className="workspace-head"><div><span className="eyebrow">DATA PLANE</span><h1>Transfers</h1><p>Multipart progress for uploads started in this browser session.</p></div></div><div className="panel big"><TransferQueue items={transfers}/></div></section>}

      {page === 'Storage Nodes' && <section className="workspace network-page"><div className="network-bg"/><div className="workspace-head"><div><span className="eyebrow">PLACEMENT SIMULATOR</span><h1>Storage nodes</h1><p>Virtual-node consistent hashing with three-way replica placement across simulated failure domains.</p></div></div><div className="panel topology-large"><NodeTopology nodes={nodes} onToggle={toggleNode}/></div></section>}

      {page === 'Shared Links' && <section className="workspace"><div className="workspace-head"><div><span className="eyebrow">SHARED LINKS</span><h1>Expiring access</h1><p>Review and revoke temporary download links.</p></div></div>{user ? <ShareList shares={shares} onRevoke={revokeShare}/> : <AuthRequired onOpen={() => setAuth(true)} text="Sign in to manage share links."/>}</section>}

      {page === 'Activity' && <section className="workspace"><div className="workspace-head"><div><span className="eyebrow">EVENT PIPELINE</span><h1>Activity</h1><p>Kafka-backed storage, sharing, and node events.</p></div></div>{user ? <ActivityList activity={activity}/> : <AuthRequired onOpen={() => setAuth(true)} text="Sign in to view account activity."/>}</section>}
    </main>

    <aside className="telemetry">
      <div className="telemetry-head"><span>SYSTEM TELEMETRY</span><b>LIVE</b></div>
      <div className="telemetry-card"><span>SIMULATED CLUSTER</span><strong>{overview.healthyNodes} / {overview.nodes}</strong><small>healthy placement nodes</small><div className="spark">{nodes.slice(0,6).map(node => <i key={node.id} style={{height:`${Math.max(18,node.capacityPct)}%`, opacity: node.status === 'healthy' ? 1 : .2}}/>)}</div></div>
      <div className="telemetry-card"><span>API ROUND TRIP</span><strong>{apiRtt ?? '--'} <em>ms</em></strong><small>public telemetry refresh</small></div>
      <div className="telemetry-card"><span>REPLICA PLACEMENT</span><strong>{overview.replicationFactor}x</strong><small>simulated copies per hash</small></div>
      <div className="telemetry-card edge"><span>INTEGRITY</span><strong>SHA-256</strong><small>server-side verification</small></div>
      <div className="telemetry-log"><span>EVENT STREAM</span>{user ? activity.slice(0,4).map(event => <p key={event.id}><i/><span>{event.type.toLowerCase().replaceAll('_',' ')}</span><small>{new Date(event.createdAt).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'})}</small></p>) : <p className="muted-event">Sign in to view Kafka activity.</p>}</div>
    </aside>

    {auth && <AuthPanel onClose={email => {setAuth(false); if (email) setUser(email);}}/>}
  </div>;
}

function AuthRequired({onOpen, text}:{onOpen:()=>void; text:string}) {
  return <div className="empty-state"><LockKeyhole size={44}/><h3>Authentication required</h3><p>{text}</p><button className="primary" onClick={onOpen}>Sign in</button></div>;
}

function ActivityList({activity}:{activity: AuditEvent[]}) {
  if (!activity.length) return <div className="empty-state"><Activity size={44}/><h3>No events yet</h3><p>Storage and infrastructure events will appear here.</p></div>;
  return <div className="activity-list">{activity.map(event => <div className="activity-row" key={event.id}><i/><div><b>{event.type.replaceAll('_',' ')}</b><span>{event.detail}</span></div><time>{new Date(event.createdAt).toLocaleString()}</time></div>)}</div>;
}

function ShareList({shares,onRevoke}:{shares: ShareItem[]; onRevoke:(id:string)=>void}) {
  if (!shares.length) return <div className="empty-state"><Share2 size={44}/><h3>No active links</h3><p>Create a 24-hour link from the Files workspace.</p></div>;
  return <div className="share-list">{shares.map(share => <div className="share-row" key={share.id}><div><b>{share.fileName}</b><span>Expires {new Date(share.expiresAt).toLocaleString()}</span></div><button className="outline" onClick={() => onRevoke(share.id)}>Revoke</button></div>)}</div>;
}

export default App;
