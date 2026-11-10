export type FileItem = {
  id: string;
  fileName: string;
  logicalPath: string;
  contentHash: string;
  sizeBytes: number;
  versionNumber: number;
  createdAt: string;
};

export type NodeInfo = {
  id: string;
  region: string;
  status: 'healthy' | 'offline';
  capacityPct: number;
};

export type Overview = {
  product: string;
  nodes: number;
  healthyNodes: number;
  replicationFactor: number;
  hashing: string;
  fileVersions: number;
  uniqueObjects: number;
  logicalBytes: number;
  physicalBytes: number;
  dedupSavingsPct: number;
};

export type AuditEvent = {
  id: string;
  type: string;
  detail: string;
  createdAt: string;
};

export type UploadSession = {
  id: string;
  owner: string;
  fileName: string;
  logicalPath: string;
  totalChunks: number;
  received: number[];
};

export type TransferItem = {
  id: string;
  name: string;
  progress: number;
  status: 'uploading' | 'verifying' | 'complete' | 'failed';
};

export type ShareItem = {
  id: string;
  fileId: string;
  fileName: string;
  expiresAt: string;
  createdAt: string;
};
