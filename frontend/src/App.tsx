import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { t } from './i18n/sr';
import AdministratorsPage from './pages/admin/AdministratorsPage';
import AdminAppointmentsPage from './pages/admin/AdminAppointmentsPage';
import AdminCalendarPage from './pages/admin/AdminCalendarPage';
import AdminCompaniesPage from './pages/admin/AdminCompaniesPage';
import AdminCompanyEditPage from './pages/admin/AdminCompanyEditPage';
import AdminLoginPage from './pages/admin/AdminLoginPage';
import AdminNewAppointmentPage from './pages/admin/AdminNewAppointmentPage';
import AdminVerifyPage from './pages/admin/AdminVerifyPage';
import RequireAdmin from './pages/admin/RequireAdmin';
import BookingPage from './pages/public/BookingPage';
import CompanyListPage from './pages/public/CompanyListPage';

export default function App() {
  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">
          {t.appName}
        </Link>
        <Link to="/admin">{t.nav.admin}</Link>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<CompanyListPage />} />
          <Route path="/companies/:companyId" element={<BookingPage />} />
          <Route path="/admin/login" element={<AdminLoginPage />} />
          <Route path="/admin/verify" element={<AdminVerifyPage />} />
          <Route path="/admin" element={<Navigate to="/admin/appointments" replace />} />
          <Route path="/admin/companies" element={<RequireAdmin><AdminCompaniesPage /></RequireAdmin>} />
          <Route path="/admin/companies/:companyId" element={<RequireAdmin><AdminCompanyEditPage /></RequireAdmin>} />
          <Route path="/admin/appointments" element={<RequireAdmin><AdminAppointmentsPage /></RequireAdmin>} />
          <Route path="/admin/appointments/new" element={<RequireAdmin><AdminNewAppointmentPage /></RequireAdmin>} />
          <Route path="/admin/administrators" element={<RequireAdmin><AdministratorsPage /></RequireAdmin>} />
          <Route path="/admin/calendar" element={<RequireAdmin><AdminCalendarPage /></RequireAdmin>} />
          <Route
            path="*"
            element={
              <p>
                {t.common.notFound} <Link to="/">{t.common.goHome}</Link>
              </p>
            }
          />
        </Routes>
      </main>
    </>
  );
}
