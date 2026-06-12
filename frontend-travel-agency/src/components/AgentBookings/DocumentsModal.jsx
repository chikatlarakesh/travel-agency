import { useState, useEffect } from 'react';
import { getAgentBookingDocuments } from '../../services/agentBookingService';

const DocumentsModal = ({ bookingId, onClose }) => {
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAgentBookingDocuments(bookingId)
      .then((data) => {
        const all = [];
        if (data.payments) {
          data.payments.forEach((d) => all.push({ id: d.id, name: d.fileName, size: d.fileSize, content: d.content, fileType: d.fileType }));
        }
        if (data.guestDocuments) {
          data.guestDocuments.forEach((guest) => {
            (guest.documents || []).forEach((d) => all.push({ id: d.id, name: d.fileName, size: d.fileSize, content: d.content, fileType: d.fileType }));
          });
        }
        setDocs(all);
      })
      .catch(() => setDocs([]))
      .finally(() => setLoading(false));
  }, [bookingId]);

  const handleDownload = (doc) => {
    if (!doc.content) return;
    const mimeType = doc.fileType ? `application/${doc.fileType}` : 'application/octet-stream';
    const byteChars = atob(doc.content);
    const byteArray = new Uint8Array(byteChars.length);
    for (let i = 0; i < byteChars.length; i++) byteArray[i] = byteChars.charCodeAt(i);
    const blob = new Blob([byteArray], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = doc.name;
    a.click();
    URL.revokeObjectURL(url);
  };

  const formatSize = (bytes) => {
    if (!bytes) return null;
    if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${Math.round(bytes / 1024)} KB`;
  };

  return (
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 1000,
        background: 'rgba(11, 56, 87, 0.32)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: '#FFFFFF', borderRadius: '12px',
          padding: '24px', width: '480px', maxWidth: '90vw',
          boxShadow: '0px 8px 32px rgba(11, 56, 87, 0.18)',
          display: 'flex', flexDirection: 'column', gap: '20px',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '20px', fontWeight: 800, color: '#0B3857', fontFamily: 'Nunito, sans-serif' }}>
            Documents
          </span>
          <button
            type="button"
            onClick={onClose}
            style={{
              background: 'none', border: 'none', cursor: 'pointer',
              padding: '4px', display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
            aria-label="Close"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M15 5L5 15M5 5l10 10" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round"/>
            </svg>
          </button>
        </div>

        {/* Body */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '24px 0', color: '#677883', fontFamily: 'Nunito, sans-serif' }}>
            Loading…
          </div>
        ) : docs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px 0', color: '#677883', fontFamily: 'Nunito, sans-serif' }}>
            No documents found.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {docs.map((doc) => (
              <div
                key={doc.id}
                style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '10px 12px', borderRadius: '8px',
                  border: '1px solid #E8F0F5', background: '#FAFCFD',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                    <path d="M4 2h8l4 4v12a1 1 0 01-1 1H4a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="#0B3857" strokeWidth="1.5"/>
                    <path d="M12 2v4h4" stroke="#0B3857" strokeWidth="1.5" strokeLinejoin="round"/>
                  </svg>
                  <div>
                    <div style={{ fontSize: '14px', fontWeight: 600, color: '#0B3857', fontFamily: 'Nunito, sans-serif' }}>
                      {doc.name}
                    </div>
                    {formatSize(doc.size) && (
                      <div style={{ fontSize: '12px', color: '#677883', fontFamily: 'Nunito, sans-serif' }}>
                        {formatSize(doc.size)}
                      </div>
                    )}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => handleDownload(doc)}
                  disabled={!doc.content}
                  style={{
                    background: 'none', border: 'none', cursor: doc.content ? 'pointer' : 'default',
                    fontSize: '14px', fontWeight: 700, color: doc.content ? '#027EAC' : '#aaa',
                    fontFamily: 'Nunito, sans-serif', padding: 0, textDecoration: 'none',
                  }}
                  onMouseEnter={(e) => { if (doc.content) e.target.style.textDecoration = 'underline'; }}
                  onMouseLeave={(e) => { e.target.style.textDecoration = 'none'; }}
                >
                  Download
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default DocumentsModal;
