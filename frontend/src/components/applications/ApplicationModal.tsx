import React, { useState, useEffect } from 'react';
import {
  ApplicationStatus,
  JobApplication,
  JobApplicationRequest,
} from '../../types/application';
import { Modal } from '../common/Modal';
import { Input } from '../common/Input';
import { Select } from '../common/Select';
import { Button } from '../common/Button';
import { getErrorMessage } from '../../api/client';
import axios from 'axios';
import { ApiErrorResponse } from '../../types/api';

interface ApplicationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: JobApplicationRequest) => Promise<void>;
  application?: JobApplication | null;
}

const statusOptions: { value: ApplicationStatus; label: string }[] = [
  { value: 'APPLIED', label: 'Applied' },
  { value: 'ASSESSMENT', label: 'Assessment' },
  { value: 'INTERVIEW', label: 'Interview' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'SELECTED', label: 'Selected' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
];

export const ApplicationModal: React.FC<ApplicationModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  application,
}) => {
  const isEditing = !!application;

  const [formData, setFormData] = useState<JobApplicationRequest>({
    companyName: '',
    jobTitle: '',
    location: '',
    jobUrl: '',
    status: 'APPLIED',
    applicationDate: new Date().toISOString().split('T')[0],
    notes: '',
    jobDescription: '',
    deadline: '',
  });

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (application) {
      setFormData({
        companyName: application.companyName || '',
        jobTitle: application.jobTitle || '',
        location: application.location || '',
        jobUrl: application.jobUrl || '',
        status: application.status || 'APPLIED',
        applicationDate: application.applicationDate || new Date().toISOString().split('T')[0],
        notes: application.notes || '',
        jobDescription: application.jobDescription || '',
        deadline: application.deadline || '',
      });
    } else {
      setFormData({
        companyName: '',
        jobTitle: '',
        location: '',
        jobUrl: '',
        status: 'APPLIED',
        applicationDate: new Date().toISOString().split('T')[0],
        notes: '',
        jobDescription: '',
        deadline: '',
      });
    }
    setFieldErrors({});
    setGeneralError(null);
  }, [application, isOpen]);

  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.companyName.trim()) {
      errors.companyName = 'Company name is required';
    }
    if (!formData.jobTitle.trim()) {
      errors.jobTitle = 'Job title is required';
    }
    if (formData.jobUrl && formData.jobUrl.trim()) {
      try {
        new URL(formData.jobUrl.trim());
      } catch {
        errors.jobUrl = 'Please enter a valid URL (e.g. https://...)';
      }
    }
    if (formData.applicationDate) {
      const selected = new Date(formData.applicationDate);
      const today = new Date();
      today.setHours(23, 59, 59, 999);
      if (selected > today) {
        errors.applicationDate = 'Application date cannot be in the future';
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGeneralError(null);

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await onSubmit({
        companyName: formData.companyName.trim(),
        jobTitle: formData.jobTitle.trim(),
        location: formData.location?.trim() || null,
        jobUrl: formData.jobUrl?.trim() || null,
        status: formData.status,
        applicationDate: formData.applicationDate || null,
        notes: formData.notes?.trim() || null,
        jobDescription: formData.jobDescription?.trim() || null,
        deadline: formData.deadline || null,
      });
      onClose();
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const errorData = err.response?.data as ApiErrorResponse | undefined;
        if (errorData?.validationErrors) {
          setFieldErrors(errorData.validationErrors);
        } else {
          setGeneralError(getErrorMessage(err));
        }
      } else {
        setGeneralError('Failed to save application.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Edit Application' : 'Track New Application'}
      description={
        isEditing
          ? 'Update the details and hiring stage for this application.'
          : 'Log a new job application to keep your search organized.'
      }
      maxWidth="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {generalError && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
            {generalError}
          </div>
        )}

        {/* Company Name & Job Title */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Company Name"
            required
            placeholder="e.g. Google, Stripe"
            value={formData.companyName}
            onChange={(e) => {
              setFormData({ ...formData, companyName: e.target.value });
              if (fieldErrors.companyName) {
                setFieldErrors({ ...fieldErrors, companyName: '' });
              }
            }}
            error={fieldErrors.companyName}
          />

          <Input
            label="Job Title"
            required
            placeholder="e.g. Senior Frontend Engineer"
            value={formData.jobTitle}
            onChange={(e) => {
              setFormData({ ...formData, jobTitle: e.target.value });
              if (fieldErrors.jobTitle) {
                setFieldErrors({ ...fieldErrors, jobTitle: '' });
              }
            }}
            error={fieldErrors.jobTitle}
          />
        </div>

        {/* Location & Status */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Location"
            placeholder="e.g. Remote, New York, NY"
            value={formData.location || ''}
            onChange={(e) => setFormData({ ...formData, location: e.target.value })}
            error={fieldErrors.location}
          />

          <Select
            label="Application Status"
            required
            value={formData.status}
            onChange={(e) =>
              setFormData({ ...formData, status: e.target.value as ApplicationStatus })
            }
            options={statusOptions}
            error={fieldErrors.status}
          />
        </div>

        {/* Job URL & Application Date */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Job Posting URL"
            type="url"
            placeholder="https://company.com/jobs/123"
            value={formData.jobUrl || ''}
            onChange={(e) => {
              setFormData({ ...formData, jobUrl: e.target.value });
              if (fieldErrors.jobUrl) {
                setFieldErrors({ ...fieldErrors, jobUrl: '' });
              }
            }}
            error={fieldErrors.jobUrl}
          />

          <Input
            label="Application Date"
            type="date"
            value={formData.applicationDate || ''}
            onChange={(e) => {
              setFormData({ ...formData, applicationDate: e.target.value });
              if (fieldErrors.applicationDate) {
                setFieldErrors({ ...fieldErrors, applicationDate: '' });
              }
            }}
            error={fieldErrors.applicationDate}
          />
        </div>

        {/* Deadline */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Deadline / Next Milestone"
            type="date"
            placeholder="Application deadline or next round"
            value={formData.deadline || ''}
            onChange={(e) => setFormData({ ...formData, deadline: e.target.value })}
            error={fieldErrors.deadline}
          />
        </div>

        {/* Job Description */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1.5">
            Job Description & Responsibilities
          </label>
          <textarea
            rows={3}
            placeholder="Key responsibilities, required qualifications, tech stack..."
            value={formData.jobDescription || ''}
            onChange={(e) => setFormData({ ...formData, jobDescription: e.target.value })}
            className="w-full rounded-lg border border-slate-300 p-3 text-sm focus:border-brand-500 focus:ring-2 focus:ring-brand-100 outline-none transition-all placeholder:text-slate-400"
          />
        </div>

        {/* Notes */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1.5">
            Notes & Next Steps
          </label>
          <textarea
            rows={3}
            placeholder="Referral name, salary range, interview rounds, recruiter contacts..."
            value={formData.notes || ''}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            className="w-full rounded-lg border border-slate-300 p-3 text-sm focus:border-brand-500 focus:ring-2 focus:ring-brand-100 outline-none transition-all placeholder:text-slate-400"
          />
        </div>

        {/* Action buttons */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
          <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={isSubmitting}>
            {isEditing ? 'Save Changes' : 'Create Application'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
