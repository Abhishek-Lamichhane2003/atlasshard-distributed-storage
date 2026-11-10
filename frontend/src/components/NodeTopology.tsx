import { Server, Zap } from 'lucide-react';
import type { NodeInfo } from '../types';

const positions = [
  [50, 12],
  [84, 30],
  [82, 70],
  [50, 88],
  [16, 70],
  [18, 30],
];

export default function NodeTopology({
  nodes,
  onToggle,
}: {
  nodes: NodeInfo[];
  onToggle: (id: string) => void;
}) {
  return <div className="topology">
    <svg viewBox="0 0 100 100" className="links">
      {positions.map((position, index) => <line
        key={index}
        x1="50"
        y1="50"
        x2={position[0]}
        y2={position[1]}
      />)}
    </svg>
    <div className="core">
      <Zap size={20}/>
      <strong>HASH RING</strong>
      <span>RF 3</span>
    </div>
    {nodes.map((node, index) => {
      const position = positions[index % positions.length];
      return <button
        key={node.id}
        className={`node ${node.status}`}
        style={{left: `${position[0]}%`, top: `${position[1]}%`}}
        onClick={() => onToggle(node.id)}
      >
        <Server size={17}/>
        <b>{node.id}</b>
        <small>{node.region}</small>
        <i>{node.capacityPct}%</i>
      </button>;
    })}
  </div>;
}
