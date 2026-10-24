// Identity & Access → Sessions
import { useEffect, useState } from 'react';
import { sessionAdminService } from '../../../services/identityService';
import { Avatar, Pagination, EmptyState, ConfirmDialog } from '../shared/AdminUi';
import { PAGE_SIZE, useDebounce, useApiQuery, formatDateTime } from '../shared/adminUiUtils';
import './IdentityAccess.css';

const SORTABLE = { loginAt: 'Login At', expireAt: 'Expires At', ipAddress: 'IP Address' };

export default function AdminSessionsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState({ by: 'loginAt', order: 'desc' });
  const debouncedSearch = useDebounce(search);

  // Row selection
  const [selected, setSelected] = useState(new Set());

  // Confirm dialogs
  const [terminating, setTerminating] = useState(null);   // single session
  const [terminatingMany, setTerminatingMany] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    document.title = 'Sessions - Identity & Access';
  }, []);

  const query = {
    page, size: PAGE_SIZE, search: debouncedSearch,
    sortBy: sort.by, sortOrder: sort.order,
  };
  const { response, loading, reload } = useApiQuery(
    () => sessionAdminService.list(query),
    JSON.stringify(query),
  );
  const sessions = response?.ok ? response.data.data || [] : [];
  const pagination = response?.ok ? response.data.pagination : null;

  // Reset to page 0 on search/sort change
  const updateSearch = (value) => { setSearch(value); setPage(0); };

  const toggleSort = (by) => {
    setSort(prev => ({ by, order: prev.by === by && prev.order === 'asc' ? 'desc' : 'asc' }));
    setPage(0);
  };

  // ── Selection ────────────────────────────────────────────────────────────
  const allIds = sessions.map(s => s.id);
  const allSelected = allIds.length > 0 && allIds.every(id => selected.has(id));
  const someSelected = allIds.some(id => selected.has(id));

  const toggleAll = () => {
    if (allSelected) {
      setSelected(prev => { const next = new Set(prev); allIds.forEach(id => next.delete(id)); return next; });
    } else {
      setSelected(prev => new Set([...prev, ...allIds]));
    }
  };

  const toggleRow = (id) => {
    setSelected(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  // ── Terminate single ─────────────────────────────────────────────────────
  const handleTerminate = async () => {
    if (!terminating) return;
    setBusy(true);
    const res = await sessionAdminService.terminate(terminating.id);
    setBusy(false);
    if (res.ok) {
      window.showSuccess?.('Session terminated');
      setSelected(prev => { const next = new Set(prev); next.delete(terminating.id); return next; });
      setTerminating(null);
      reload();
    }
  };

  // ── Terminate many ───────────────────────────────────────────────────────
  const handleTerminateMany = async () => {
    const sessionIds = [...selected];
    if (!sessionIds.length) return;
    setBusy(true);
    const res = await sessionAdminService.terminateMany(sessionIds);
    setBusy(false);
    if (res.ok) {
      const count = res.data?.data?.terminated ?? sessionIds.length;
      window.showSuccess?.(`${count} session${count !== 1 ? 's' : ''} terminated`);
      setSelected(new Set());
      setTerminatingMany(false);
      reload();
    }
  };

  const selectedCount = selected.size;

  return (
    <div className="admin-ui-page">
      <header className="admin-ui-header">
        <div>
          <span className="admin-ui-eyebrow">
            <i className="bi bi-shield-lock"></i> Identity &amp; Access
          </span>
          <h1>Sessions</h1>
          <p>View and terminate active login sessions across all users.</p>
        </div>
      </header>

      <div className="admin-ui-toolbar">
        <label className="admin-ui-search">
          <i className="bi bi-search"></i>
          <input
            type="search"
            placeholder="Search by IP address…"
            value={search}
            onChange={(e) => updateSearch(e.target.value)}
          />
        </label>

        {selectedCount > 0 && (
          <button
            type="button"
            className="admin-ui-btn admin-ui-btn--danger"
            onClick={() => setTerminatingMany(true)}
          >
            <i className="bi bi-x-circle"></i> Terminate {selectedCount} selected
          </button>
        )}
      </div>

      <div className="admin-ui-card">
        <div className="admin-ui-table-wrap">
          <table className="admin-ui-table">
            <thead>
              <tr>
                <th style={{ width: 40 }}>
                  <input
                    type="checkbox"
                    checked={allSelected}
                    ref={el => { if (el) el.indeterminate = someSelected && !allSelected; }}
                    onChange={toggleAll}
                    aria-label="Select all sessions on this page"
                  />
                </th>
                <th>User</th>
                <SortHeader by="ipAddress" sort={sort} onSort={toggleSort} />
                <SortHeader by="loginAt" sort={sort} onSort={toggleSort} />
                <SortHeader by="expireAt" sort={sort} onSort={toggleSort} />
                <th className="admin-ui-col-actions">
                  <span className="visually-hidden">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {loading && sessions.length === 0 &&
                [...Array(5)].map((_, i) => (
                  <tr key={i} className="admin-ui-skeleton-row">
                    <td colSpan={6}><span></span></td>
                  </tr>
                ))
              }
              {sessions.map(session => (
                <tr key={session.id} className={selected.has(session.id) ? 'is-selected' : ''}>
                  <td onClick={(e) => e.stopPropagation()}>
                    <input
                      type="checkbox"
                      checked={selected.has(session.id)}
                      onChange={() => toggleRow(session.id)}
                      aria-label={`Select session for ${session.userEmail || session.userId}`}
                    />
                  </td>
                  <td>
                    <div className="admin-ui-user">
                      <Avatar user={{ name: session.userName, email: session.userEmail, avatar: session.userAvatar }} />
                      <div>
                        <strong>{session.userName || 'Unnamed'}</strong>
                        <span>{session.userEmail}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className="admin-iam-signin">
                      <i className="bi bi-geo-alt"></i>
                      {session.ipAddress || '—'}
                    </span>
                  </td>
                  <td className="admin-ui-muted">{formatDateTime(session.loginAt)}</td>
                  <td className="admin-ui-muted">
                    <ExpiryCell expireAt={session.expireAt} />
                  </td>
                  <td className="admin-ui-col-actions" onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="admin-ui-icon-btn admin-ui-icon-btn--danger"
                      title="Terminate session"
                      onClick={() => setTerminating(session)}
                    >
                      <i className="bi bi-x-circle"></i>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {!loading && sessions.length === 0 && (
            <EmptyState
              icon="bi-window-x"
              title="No active sessions"
              text="Try adjusting your search filters."
            />
          )}
        </div>
        <Pagination pagination={pagination} onChange={setPage} />
      </div>

      {/* Single terminate confirm */}
      {terminating && (
        <ConfirmDialog
          title="Terminate session"
          message={
            <>
              Terminate the session for <strong>{terminating.userEmail || terminating.userId}</strong>
              {terminating.ipAddress ? <> from <strong>{terminating.ipAddress}</strong></> : null}?
              The user will be signed out on that device.
            </>
          }
          confirmLabel="Terminate"
          busy={busy}
          onConfirm={handleTerminate}
          onClose={() => setTerminating(null)}
        />
      )}

      {/* Batch terminate confirm */}
      {terminatingMany && (
        <ConfirmDialog
          title="Terminate selected sessions"
          message={
            <>
              Terminate <strong>{selectedCount}</strong> selected session{selectedCount !== 1 ? 's' : ''}?
              The affected users will be signed out on those devices.
            </>
          }
          confirmLabel={`Terminate ${selectedCount}`}
          busy={busy}
          onConfirm={handleTerminateMany}
          onClose={() => setTerminatingMany(false)}
        />
      )}
    </div>
  );
}

// ── Sub-components ────────────────────────────────────────────────────────────

function SortHeader({ by, sort, onSort }) {
  const active = sort.by === by;
  return (
    <th aria-sort={active ? (sort.order === 'asc' ? 'ascending' : 'descending') : 'none'}>
      <button
        type="button"
        className={`admin-ui-sort${active ? ' active' : ''}`}
        onClick={() => onSort(by)}
      >
        {SORTABLE[by]}
        <i className={`bi ${active
          ? (sort.order === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down')
          : 'bi-arrow-down-up'}`}
        ></i>
      </button>
    </th>
  );
}

/** Shows the expiry timestamp; red tint when already expired. */
function ExpiryCell({ expireAt }) {
  if (!expireAt) return <span className="admin-ui-muted">—</span>;
  const expired = new Date(expireAt) < new Date();
  return (
    <span style={expired ? { color: 'var(--adm-danger, #e11d48)' } : undefined}>
      {formatDateTime(expireAt)}
      {expired && <> <i className="bi bi-clock-history" title="Expired"></i></>}
    </span>
  );
}
