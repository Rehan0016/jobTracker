import React, { useState, useEffect, useCallback } from 'react';
import {
  Briefcase,
  Clock,
  MessageSquare,
  Award,
  XCircle,
  Plus,
  RefreshCw,
  ArrowRight,
  TrendingUp,
  Calendar,
  MapPin,
  ExternalLink,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import {
  ApplicationStatus,
  DashboardStats,
  JobApplication,
  JobApplicationRequest,
} from '../types/application';
import {
  createApplicationApi,
  deleteApplicationApi,
  getDashboardStatsApi,
  getApplicationsPagedApi,
  getUpcomingInterviewsApi,
  updateApplicationApi,
} from '../api/applications';
import { StatCard } from '../components/dashboard/StatCard';
import { StatusDistribution } from '../components/dashboard/StatusDistribution';
import { ApplicationTable } from '../components/applications/ApplicationTable';
import { ApplicationModal } from '../components/applications/ApplicationModal';
import { DeleteConfirmModal } from '../components/applications/DeleteConfirmModal';
import { Button } from '../components/common/Button';
import { Spinner } from '../components/common/Spinner';
import { EmptyState } from '../components/common/EmptyState';
import { Badge } from '../components/common/Badge';
import { getErrorMessage } from '../api/client';

export const DashboardPage: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats>({
    totalApplications: 0,
    applied: 0,
    interviews: 0,
    rejected: 0,
    selected: 0,
    offers: 0,
    assessment: 0,
    withdrawn: 0,
    successRate: 0,
  });

  const [recentApplications, setRecentApplications] = useState<JobApplication[]>([]);
  const [upcomingInterviews, setUpcomingInterviews] = useState<JobApplication[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modals state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingApplication, setEditingApplication] = useState<JobApplication | null>(null);
  const [deletingApplication, setDeletingApplication] = useState<JobApplication | null>(null);

  const fetchDashboardData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [statsData, pagedData] = await Promise.all([
        getDashboardStatsApi(),
        getApplicationsPagedApi({ page: 0, size: 5, sort: 'applicationDate,desc' }),
      ]);
      setStats(statsData);
      setRecentApplications(pagedData?.content || []);

      try {
        if (typeof getUpcomingInterviewsApi === 'function') {
          const interviews = await getUpcomingInterviewsApi();
          if (Array.isArray(interviews)) {
            setUpcomingInterviews(interviews);
          }
        }
      } catch {
        setUpcomingInterviews([]);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

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
      await fetchDashboardData();
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
    await fetchDashboardData();
  };

  const handleDeleteConfirm = async () => {
    if (!deletingApplication) return;
    await deleteApplicationApi(deletingApplication.id);
    await fetchDashboardData();
  };

  if (isLoading && stats.totalApplications === 0 && recentApplications.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Spinner size="lg" label="Loading dashboard metrics..." />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Top Banner & Quick Action */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 tracking-tight">Overview</h2>
          <p className="text-sm text-slate-500 mt-1">
            Real-time status of your active job applications and interview progression.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="md"
            onClick={fetchDashboardData}
            leftIcon={<RefreshCw className="w-4 h-4" />}
            title="Refresh statistics"
          >
            Refresh
          </Button>
          <Button
            variant="primary"
            size="md"
            onClick={() => {
              setEditingApplication(null);
              setIsModalOpen(true);
            }}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Add Application
          </Button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm font-medium flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={fetchDashboardData}
            className="text-xs text-rose-800 underline font-semibold hover:text-rose-900 cursor-pointer"
          >
            Try Again
          </button>
        </div>
      )}

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        <StatCard
          title="Total Applications"
          value={stats.totalApplications}
          color="indigo"
          icon={<Briefcase className="w-6 h-6" />}
          subtitle="All recorded submissions"
        />
        <StatCard
          title="Applied"
          value={stats.applied}
          color="blue"
          icon={<Clock className="w-6 h-6" />}
          subtitle="Awaiting response"
        />
        <StatCard
          title="Interviews"
          value={stats.interviews}
          color="purple"
          icon={<MessageSquare className="w-6 h-6" />}
          subtitle="In active evaluation"
        />
        <StatCard
          title="Offers"
          value={stats.offers}
          color="emerald"
          icon={<Award className="w-6 h-6" />}
          subtitle="Official offers extended"
        />
        <StatCard
          title="Rejected"
          value={stats.rejected}
          color="rose"
          icon={<XCircle className="w-6 h-6" />}
          subtitle="Unsuccessful rounds"
        />
        <StatCard
          title="Success Rate"
          value={`${stats.successRate ?? (stats.totalApplications > 0 ? (((stats.offers + stats.selected) / stats.totalApplications) * 100).toFixed(1) : 0)}%`}
          color="teal"
          icon={<TrendingUp className="w-6 h-6" />}
          subtitle="Conversion to offer"
        />
      </div>

      {/* Pipeline Status Distribution Visual */}
      <StatusDistribution stats={stats} />

      {/* Upcoming Interviews Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-slate-900">Upcoming Interviews</h3>
            <p className="text-xs text-slate-500">Active evaluation rounds and scheduled meetings</p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-purple-50 text-purple-700 border border-purple-200">
            {upcomingInterviews.length} Active
          </span>
        </div>

        {upcomingInterviews.length === 0 ? (
          <div className="p-6 rounded-2xl bg-white border border-slate-200/80 text-center">
            <p className="text-sm font-medium text-slate-700">No active interviews currently queued</p>
            <p className="text-xs text-slate-400 mt-1">Applications in Interview status will appear here with round details and deadlines.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {upcomingInterviews.map((app) => (
              <div
                key={app.id}
                className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-purple-200 transition-all space-y-3"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h4 className="font-bold text-slate-900 text-sm">{app.companyName}</h4>
                    <p className="text-xs font-medium text-slate-600">{app.jobTitle}</p>
                  </div>
                  <Badge status={app.status} size="sm" />
                </div>

                <div className="space-y-1.5 text-xs text-slate-500">
                  {app.location && (
                    <div className="flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      <span>{app.location}</span>
                    </div>
                  )}
                  {app.deadline && (
                    <div className="flex items-center gap-1.5 text-amber-700 font-medium">
                      <Calendar className="w-3.5 h-3.5 text-amber-500" />
                      <span>Milestone: {app.deadline}</span>
                    </div>
                  )}
                  {app.notes && (
                    <p className="text-xs text-slate-400 line-clamp-2 italic">
                      "{app.notes}"
                    </p>
                  )}
                </div>

                <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
                  {app.jobUrl ? (
                    <a
                      href={app.jobUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-brand-600 hover:text-brand-700 font-medium"
                    >
                      <span>Posting</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  ) : <span />}
                  <button
                    onClick={() => {
                      setEditingApplication(app);
                      setIsModalOpen(true);
                    }}
                    className="text-slate-600 hover:text-slate-900 font-semibold cursor-pointer"
                  >
                    Edit
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Recent Applications Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-slate-900">Recent Applications</h3>
            <p className="text-xs text-slate-500">Latest submissions and stage updates</p>
          </div>
          <Link
            to="/applications"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-700 transition-colors"
          >
            <span>View All Applications</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {recentApplications.length === 0 ? (
          <EmptyState
            title="No applications tracked yet"
            description="Start tracking your career opportunities. Add your first job application today!"
            actionText="Add First Application"
            onAction={() => {
              setEditingApplication(null);
              setIsModalOpen(true);
            }}
          />
        ) : (
          <ApplicationTable
            applications={recentApplications}
            onEdit={(app) => {
              setEditingApplication(app);
              setIsModalOpen(true);
            }}
            onDelete={(app) => {
              setDeletingApplication(app);
            }}
            onStatusChange={handleQuickStatusChange}
            sortBy="applicationDate"
            sortDirection="desc"
            onSort={() => {}}
          />
        )}
      </div>

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
