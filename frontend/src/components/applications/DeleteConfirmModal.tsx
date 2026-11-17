import React, { useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { JobApplication } from '../../types/application';

interface DeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
  application?: JobApplication | null;
}

export const DeleteConfirmModal: React.FC<DeleteConfirmModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  application,
}) => {
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await onConfirm();
      onClose();
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Delete Application"
      maxWidth="sm"
    >
      <div className="flex flex-col items-center text-center">
        <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mb-4 border border-rose-100">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <p className="text-sm text-slate-600 mb-2">
          Are you sure you want to delete the application for{' '}
          <strong className="text-slate-900 font-semibold">{application?.companyName}</strong> -{' '}
          <span className="text-slate-700">{application?.jobTitle}</span>?
        </p>
        <p className="text-xs text-slate-400 mb-6">
          This action cannot be undone and will permanently remove this record from your tracker.
        </p>

        <div className="flex items-center gap-3 w-full">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={isDeleting}
            className="flex-1"
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant="danger"
            onClick={handleDelete}
            isLoading={isDeleting}
            className="flex-1"
          >
            Delete
          </Button>
        </div>
      </div>
    </Modal>
  );
};
