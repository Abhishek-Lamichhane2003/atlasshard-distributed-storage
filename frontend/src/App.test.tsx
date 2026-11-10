import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

describe('App', () => {
  it('renders the storage control plane without fabricated telemetry', () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')));
    render(<App/>);
    expect(screen.getByText(/Store once/)).toBeInTheDocument();
    expect(screen.getByText('Node topology')).toBeInTheDocument();
    expect(screen.getAllByText('0 B').length).toBeGreaterThan(0);
    expect(screen.queryByText('24.8K')).not.toBeInTheDocument();
  });
});
