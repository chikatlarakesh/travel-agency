import { useState, useRef, useEffect } from 'react';
import { ReactComponent as CloseIcon } from '../../assets/icons/Close.svg';
import './UploadDocumentsModal.css';
import { uploadBookingDocuments, getBookingDocuments, deleteBookingDocument } from '../../services/bookingService';

const toBase64 = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      // Strip the data URL prefix "data:<mime>;base64," — keep only the base64 part
      const result = reader.result;
      resolve(result.split(',')[1]);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

const UploadDocumentsModal = ({ onClose, travelerName, bookingId, hasExistingDocuments, onSaveSuccess }) => {
  const travelerNames = travelerName
    ? travelerName.split(',').map((n) => n.trim()).filter(Boolean)
    : [''];
  const TABS = [
    ...travelerNames.map((name) => `Passport ${name}`.trim()),
    'Payment confirmation',
  ];

  const [activeTab, setActiveTab] = useState(0);
  const initialFiles = Object.fromEntries(TABS.map((_, i) => [i, []]));
  const [filesByTab, setFilesByTab] = useState(initialFiles);
  // Existing file names (from server) per tab — shown as already uploaded
  const [existingByTab, setExistingByTab] = useState(Object.fromEntries(TABS.map((_, i) => [i, []])));
  const [loadingExisting, setLoadingExisting] = useState(hasExistingDocuments);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const inputRef = useRef(null);
  const removedExistingIds = useRef([]);

  // Tab indices: 0...(travelerNames.length-1) are passport tabs, last is payment
  const paymentTabIndex = TABS.length - 1;

  // Fetch existing documents when modal opens and documents already exist
  useEffect(() => {
    if (!hasExistingDocuments || !bookingId) return;
    setLoadingExisting(true);
    getBookingDocuments(bookingId)
      .then((data) => {
        const byTab = Object.fromEntries(TABS.map((_, i) => [i, []]));
        // Map payment docs → last tab
        if (data.payments) {
          byTab[paymentTabIndex] = data.payments.map((d) => ({ id: d.id, name: d.fileName, existing: true }));
        }
        // Map guest docs → matching passport tabs by traveler name
        if (data.guestDocuments) {
          data.guestDocuments.forEach((guest) => {
            const tabIdx = travelerNames.findIndex(
              (n) => n.toLowerCase() === (guest.userName || '').toLowerCase()
            );
            if (tabIdx !== -1 && guest.documents) {
              byTab[tabIdx] = guest.documents.map((d) => ({ id: d.id, name: d.fileName, existing: true }));
            }
          });
        }
        setExistingByTab(byTab);
      })
      .catch(() => {/* silent — just don't pre-fill */})
      .finally(() => setLoadingExisting(false));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const files = filesByTab[activeTab] || [];
  const existingFiles = existingByTab[activeTab] || [];
  const allFilesForTab = (tab) => (filesByTab[tab] || []).length + (existingByTab[tab] || []).length;

  // Max files allowed per tab: 1 for each passport tab, 2 per traveler for payment tab
  const maxForTab = (tab) => tab === paymentTabIndex ? travelerNames.length * 2 : 1;
  const activeTabMax = maxForTab(activeTab);
  const activeTabCount = allFilesForTab(activeTab);
  const activeTabFull = activeTabCount >= activeTabMax;

  const handleFiles = (incoming) => {
    const arr = Array.from(incoming);
    const currentCount = allFilesForTab(activeTab);
    const remaining = activeTabMax - currentCount;
    if (remaining <= 0) return;
    const toAdd = arr.slice(0, remaining);
    setFilesByTab((prev) => ({
      ...prev,
      [activeTab]: [...(prev[activeTab] || []), ...toAdd],
    }));
  };

  const handleDrop = (e) => {
    e.preventDefault();
    handleFiles(e.dataTransfer.files);
  };

  const handleRemove = (index) => {
    setFilesByTab((prev) => ({
      ...prev,
      [activeTab]: prev[activeTab].filter((_, i) => i !== index),
    }));
  };

  const handleRemoveExisting = (index) => {
    const removed = existingByTab[activeTab]?.[index];
    if (removed?.id) {
      removedExistingIds.current = [...removedExistingIds.current, removed.id];
    }
    setExistingByTab((prev) => ({
      ...prev,
      [activeTab]: prev[activeTab].filter((_, i) => i !== index),
    }));
  };

  const canSave = TABS.every((_, i) => allFilesForTab(i) > 0);

  const handleSave = async () => {
    if (!canSave || isSaving) return;
    setSaveError(null);
    setIsSaving(true);
    try {
      // Step 1: Delete removed existing documents
      if (removedExistingIds.current.length > 0) {
        await Promise.all(
          removedExistingIds.current.map((id) => deleteBookingDocument(bookingId, id))
        );
      }

      // Step 2: Upload only NEW files (files the user just added this session)
      const paymentFiles = filesByTab[paymentTabIndex] || [];
      const payments = await Promise.all(
        paymentFiles.map(async (file) => ({
          fileName: file.name,
          type: 'PAYMENT_RECEIPT',
          base64encodedDocument: await toBase64(file),
        }))
      );

      const guestDocuments = await Promise.all(
        travelerNames.map(async (name, idx) => {
          const passportFiles = filesByTab[idx] || [];
          const documents = await Promise.all(
            passportFiles.map(async (file) => ({
              fileName: file.name,
              type: 'PASSPORT',
              base64encodedDocument: await toBase64(file),
            }))
          );
          return { userName: name, documents };
        })
      );

      const filteredGuestDocuments = guestDocuments.filter((g) => g.documents.length > 0);
      const hasNewFiles = payments.length > 0 || filteredGuestDocuments.length > 0;

      if (hasNewFiles) {
        const payload = {
          payments: payments.length > 0 ? payments : undefined,
          guestDocuments: filteredGuestDocuments.length > 0 ? filteredGuestDocuments : undefined,
        };
        await uploadBookingDocuments(bookingId, payload);
      }
      // Total remaining = existing files kept + new files added
      const keptExisting = Object.values(existingByTab).flat().length;
      const newFiles = Object.values(filesByTab).flat().length;
      onSaveSuccess?.(keptExisting + newFiles);
      onClose();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.error || 'Failed to upload documents. Please try again.';
      setSaveError(msg);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      className="upload-modal-overlay"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="upload-modal">
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', height: '40px' }}>
          <span
            className="font-nunito"
            style={{ fontSize: '24px', fontWeight: 700, lineHeight: '40px', color: '#0B3857' }}
          >
            {hasExistingDocuments ? 'Update documents' : 'Upload documents'}
          </span>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '0', display: 'flex', alignItems: 'center', justifyContent: 'center', width: '24px', height: '24px' }}
            aria-label="Close"
          >
            <CloseIcon style={{ width: '24px', height: '24px', color: '#0B3857' }} />
          </button>
        </div>

        {/* Container */}
        {loadingExisting ? (
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '200px' }}>
            <span className="font-nunito" style={{ color: '#677883', fontSize: '14px' }}>Loading documents…</span>
          </div>
        ) : (
        <div className="upload-modal-body" style={{ display: 'flex', gap: '24px', width: '100%', alignItems: 'flex-start', flex: 1, minHeight: 0 }}>
          {/* Left: document tabs */}
          <div
            className="upload-modal-tabs"
            style={{
              width: '304px',
              flexShrink: 0,
              borderRight: '1px solid #D3E1ED',
              paddingRight: '24px',
              display: 'flex',
              flexDirection: 'column',
              gap: '0px',
              alignSelf: 'stretch',
              boxSizing: 'border-box',
            }}
          >
            {TABS.map((tab, idx) => {
              const isActive = activeTab === idx;
              const hasFiles = allFilesForTab(idx) > 0;
              return (
                <button
                  key={tab}
                  onClick={() => setActiveTab(idx)}
                  style={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: '4px 0 0',
                    textAlign: 'left',
                    width: '100%',
                  }}
                >
                  {/* Row: label + optional check badge */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', paddingBottom: '8px' }}>
                    <span
                      className="font-nunito"
                      style={{
                        fontSize: '14px',
                        fontWeight: 800,
                        lineHeight: '24px',
                        color: isActive ? '#0B3857' : '#677883',
                        flexGrow: 1,
                      }}
                    >
                      {tab}
                    </span>
                    {hasFiles && (
                      <span
                        style={{
                          width: '24px',
                          height: '24px',
                          borderRadius: '100px',
                          border: '1px solid #118819',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                        }}
                      >
                        {/* Checkmark icon */}
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                          <path
                            d="M3 8L6.5 11.5L13 5"
                            stroke="#118819"
                            strokeWidth="1.16667"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      </span>
                    )}
                  </div>
                  {/* Active underline */}
                  <div
                    style={{
                      height: '5px',
                      background: isActive ? '#027EAC' : 'transparent',
                      borderRadius: '6px',
                      width: '100%',
                    }}
                  />
                </button>
              );
            })}
          </div>

          {/* Right: upload area */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px', minWidth: '260px' }}>
            {/* Upload label + limit hint */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span
                className="font-nunito"
                style={{ fontSize: '14px', fontWeight: 800, lineHeight: '24px', color: '#0B3857' }}
              >
                Add attachments
              </span>
              <span className="font-nunito" style={{ fontSize: '12px', color: '#677883', lineHeight: '16px' }}>
                {activeTabCount}/{activeTabMax} file{activeTabMax !== 1 ? 's' : ''}
              </span>
            </div>

            {/* Drop zone */}
            <div
              onDrop={!activeTabFull ? handleDrop : undefined}
              onDragOver={!activeTabFull ? (e) => e.preventDefault() : undefined}
              onClick={!activeTabFull ? () => inputRef.current?.click() : undefined}
              style={{
                border: `1px solid ${activeTabFull ? '#D3E1ED' : '#D3E1ED'}`,
                borderRadius: '8px',
                padding: '16px 12px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                cursor: activeTabFull ? 'not-allowed' : 'pointer',
                minHeight: '90px',
                background: activeTabFull ? '#F5F8FB' : '#FFFFFF',
                opacity: activeTabFull ? 0.6 : 1,
              }}
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M3 17v3a1 1 0 001 1h16a1 1 0 001-1v-3" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round"/>
                <path d="M12 3v11M8 7l4-4 4 4" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              <span className="font-nunito" style={{ fontSize: '14px', fontWeight: 700, lineHeight: '24px', color: '#0B3857', textAlign: 'center' }}>
                {activeTabFull ? `Maximum ${activeTabMax} file${activeTabMax !== 1 ? 's' : ''} reached` : 'Click to upload or drag and drop'}
              </span>
              <input
                ref={inputRef}
                type="file"
                multiple={activeTabMax > 1}
                style={{ display: 'none' }}
                onChange={(e) => handleFiles(e.target.files)}
              />
            </div>

            {/* Existing files (already on server) */}
            {existingFiles.length > 0 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '12px' }}>
                {existingFiles.map((file, idx) => (
                  <div key={`existing-${idx}`} style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', width: '100%' }}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
                      <path d="M4 2h10l6 6v14a2 2 0 01-2 2H4a2 2 0 01-2-2V4a2 2 0 012-2z" stroke="#118819" strokeWidth="1.75"/>
                      <polyline points="14 2 14 8 20 8" stroke="#118819" strokeWidth="1.75"/>
                    </svg>
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                      <span className="font-nunito truncate" style={{ fontSize: '14px', color: '#0B3857', lineHeight: '24px', fontWeight: 400 }}>
                        {file.name}
                      </span>
                      <span className="font-nunito" style={{ fontSize: '12px', color: '#118819', lineHeight: '16px' }}>Already uploaded</span>
                    </div>
                    <button
                      onClick={() => handleRemoveExisting(idx)}
                      style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, flexShrink: 0 }}
                      aria-label="Remove file"
                    >
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round"/>
                        <path d="M10 11v6M14 11v6" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round"/>
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* File list */}
            {files.length > 0 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '12px' }}>
                {files.map((file, idx) => (
                  <div
                    key={idx}
                    style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', width: '100%' }}
                  >
                    {/* File icon */}
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
                      <path d="M4 2h10l6 6v14a2 2 0 01-2 2H4a2 2 0 01-2-2V4a2 2 0 012-2z" stroke="#0B3857" strokeWidth="1.75"/>
                      <polyline points="14 2 14 8 20 8" stroke="#0B3857" strokeWidth="1.75"/>
                    </svg>
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                      <span className="font-nunito truncate" style={{ fontSize: '14px', color: '#0B3857', lineHeight: '24px', fontWeight: 400 }}>
                        {file.name}
                      </span>
                      <span className="font-nunito" style={{ fontSize: '12px', color: '#677883', lineHeight: '16px' }}>
                        {file.size ? `${Math.round(file.size / 1024)} KB` : ''}
                      </span>
                    </div>
                    {/* Delete button */}
                    <button
                      onClick={() => handleRemove(idx)}
                      style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, flexShrink: 0 }}
                      aria-label="Remove file"
                    >
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round"/>
                        <path d="M10 11v6M14 11v6" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round"/>
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
        )}

        {/* Actions */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px', width: '100%' }}>
          {saveError && (
            <span className="font-nunito" style={{ fontSize: '12px', color: '#d32f2f', lineHeight: '16px' }}>
              {saveError}
            </span>
          )}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
            <button
              onClick={onClose}
              disabled={isSaving}
              className="font-nunito"
              style={{
                height: '40px',
                padding: '8px 16px',
                borderRadius: '8px',
                border: '2px solid #027EAC',
                background: '#FFFFFF',
                color: '#027EAC',
                fontSize: '14px',
                fontWeight: 700,
                lineHeight: '24px',
                cursor: isSaving ? 'default' : 'pointer',
                width: '76px',
                boxSizing: 'border-box',
                opacity: isSaving ? 0.5 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={!canSave || isSaving}
              className="font-nunito"
              style={{
                height: '40px',
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                background: '#027EAC',
                opacity: canSave && !isSaving ? 1 : 0.5,
                color: '#FFFFFF',
                fontSize: '14px',
                fontWeight: 700,
                lineHeight: '24px',
                cursor: canSave && !isSaving ? 'pointer' : 'default',
                width: '64px',
                boxSizing: 'border-box',
              }}
            >
              {isSaving ? '...' : 'Save'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UploadDocumentsModal;

