import React, { useState, useEffect, useCallback } from 'react';
import {
  ApplicationStatus,
  JobApplication,
  JobApplicationRequest,
} from '../types/application';
import {
  createApplicationApi,
  deleteApplicationApi,
  getApplicationsPagedApi,
  updateApplicationApi,
} from '../api/applications';
import { ApplicationFilterBar } from '../components/applications/ApplicationFilterBar';
import { ApplicationTable } from '../components/applications/ApplicationTable';
import { Pagination } from '../components/applications/Pagination';
import { ApplicationModal } from '../components/applications/ApplicationModal';
import { DeleteConfirmModal } from '../components/applications/DeleteConfirmModal';
import { EmptyState } from '../components/common/EmptyState';
import { Spinner } from '../components/common/Spinner';
import { getErrorMessage } from '../api/client';

export const ApplicationsPage: React.FC = () => {
  const [applications, setApplications] = useState<JobApplication[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [search, setSearch] = useState('');
  const [companyFilter, setCompanyFilter] = useState('');
  const [titleFilter, setTitleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<ApplicationStatus | 'ALL'>('ALL');
  const [sortBy, setSortBy] = useState('applicationDate');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingApplication, setEditingApplication] = useState<JobApplication | null>(null);
  const [deletingApplication, setDeletingApplication] = useState<JobApplication | null>(null);

  const fetchApplications = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await getApplicationsPagedApi({
        page: currentPage,
        size: pageSize,
        search: search.trim() ? search.trim() : undefined,
        company: companyFilter.trim() ? companyFilter.trim() : undefined,
        title: titleFilter.trim() ? titleFilter.trim() : undefined,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        sort: `${sortBy},${sortDirection}`,
      });
      setApplications(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageSize, search, companyFilter, titleFilter, statusFilter, sortBy, sortDirection]);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(field);
      setSortDirection('asc');
    }
    setCurrentPage(0);
  };

  const handleSearchChange = (value: string) => {
    setSearch(value);
    setCurrentPage(0);
  };

  const handleCompanyChange = (value: string) => {
    setCompanyFilter(value);
    setCurrentPage(0);
  };

  const handleTitleChange = (value: string) => {
    setTitleFilter(value);
    setCurrentPage(0);
  };

  const handleStatusChange = (status: ApplicationStatus | 'ALL') => {
    setStatusFilter(status);
    setCurrentPage(0);
  };

  const handleResetFilters = () => {
    setSearch('');
    setCompanyFilter('');
    setTitleFilter('');
    setStatusFilter('ALL');
    setCurrentPage(0);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setCurrentPage(0);
  };

  const handleQuickStatusChange = async (app: JobApplication, newStatus: ApplicationStatus) => {
    try {
      await updateApplicationApi(app.id, {
        companyName: app.companyName,
        jobTitle: app.jobTitle,
        location: app.location,
        jobUrl: app.jobUrl,
        status: newStatus,
        applicationDate: app.applicationDate,
        notes: app.notes,
        jobDescription: app.jobDescription,
        deadline: app.deadline,
      });
      await fetchApplications();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleCreateOrUpdate = async (data: JobApplicationRequest) => {
    if (editingApplication) {
      await updateApplicationApi(editingApplication.id, data);
    } else {
      await createApplicationApi(data);
    }
    await fetchApplications();
  };

  const handleDeleteConfirm = async () => {
    if (!deletingApplication) return;
    await deleteApplicationApi(deletingApplication.id);
    await fetchApplications();
  };

  return (
    <div className="space-y-6">
      {/* Page Title & Stats Badge */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 pb-2">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 tracking-tight">Applications</h2>
          <p className="text-sm text-slate-500 mt-1">
            Search, filter, update, and manage every stage of your job hunt.
          </p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <ApplicationFilterBar
        search={search}
        onSearchChange={handleSearchChange}
        companyFilter={companyFilter}
        onCompanyChange={handleCompanyChange}
        titleFilter={titleFilter}
        onTitleChange={handleTitleChange}
        statusFilter={statusFilter}
        onStatusChange={handleStatusChange}
        onResetFilters={handleResetFilters}
        onNewClick={() => {
          setEditingApplication(null);
          setIsModalOpen(true);
        }}
      />

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm font-medium flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={fetchApplications}
            className="text-xs text-rose-800 underline font-semibold hover:text-rose-900 cursor-pointer"
          >
            Retry
          </button>
        </div>
      )}

      {/* Content Area */}
      {isLoading ? (
        <div className="flex items-center justify-center min-h-[40vh] bg-white rounded-2xl border border-slate-200/80 p-12">
          <Spinner size="lg" label="Loading applications..." />
        </div>
      ) : applications.length === 0 ? (
        <EmptyState
          title={
            search || companyFilter || titleFilter || statusFilter !== 'ALL'
              ? 'No matching applications found'
              : 'No applications tracked yet'
          }
          description={
            search || companyFilter || titleFilter || statusFilter !== 'ALL'
              ? 'Try adjusting your search criteria or resetting the status and text filters.'
              : 'Add your first job application to begin monitoring your career pipeline!'
          }
          actionText={
            search || companyFilter || titleFilter || statusFilter !== 'ALL' ? 'Clear Filters' : 'Add Application'
          }
          onAction={() => {
            if (search || companyFilter || titleFilter || statusFilter !== 'ALL') {
              handleResetFilters();
            } else {
              setEditingApplication(null);
              setIsModalOpen(true);
            }
          }}
        />
      ) : (
        <div className="space-y-4">
          <ApplicationTable
            applications={applications}
            onEdit={(app) => {
              setEditingApplication(app);
              setIsModalOpen(true);
            }}
            onDelete={(app) => {
              setDeletingApplication(app);
            }}
            onStatusChange={handleQuickStatusChange}
            sortBy={sortBy}
            sortDirection={sortDirection}
            onSort={handleSort}
          />

          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={setCurrentPage}
            onPageSizeChange={handlePageSizeChange}
          />
        </div>
      )}

      {/* Modals */}
      <ApplicationModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleCreateOrUpdate}
        application={editingApplication}
      />

      <DeleteConfirmModal
        isOpen={!!deletingApplication}
        onClose={() => setDeletingApplication(null)}
        onConfirm={handleDeleteConfirm}
        application={deletingApplication}
      />
    </div>
  );
};
