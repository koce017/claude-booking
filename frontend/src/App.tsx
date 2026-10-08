import { Link, Navigate, Route, Routes } from 'react-router-dom';
import CompanyListPage from './pages/public/CompanyListPage';
import BookingPage from './pages/public/BookingPage';
import AdminLoginPage from './pages/admin/AdminLoginPage';
import AdminVerifyPage from './pages/admin/AdminVerifyPage';
import AdminCompaniesPage from './pages/admin/AdminCompaniesPage';
import AdminAppointmentsPage from './pages/admin/AdminAppointmentsPage';
import RequireAdmin from './pages/admin/RequireAdmin';

export default function App() {
  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">Appointment Booking</Link>
        <Link to="/admin">Admin</Link>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<CompanyListPage />} />
          <Route path="/companies/:companyId" element={<BookingPage />} />
          <Route path="/admin/login" element={<AdminLoginPage />} />
          <Route path="/admin/verify" element={<AdminVerifyPage />} />
          <Route path="/admin" element={<Navigate to="/admin/companies" replace />} />
          <Route path="/admin/companies" element={<RequireAdmin><AdminCompaniesPage /></RequireAdmin>} />
          <Route path="/admin/appointments" element={<RequireAdmin><AdminAppointmentsPage /></RequireAdmin>} />
          <Route path="*" element={<p>Page not found. <Link to="/">Go home</Link></p>} />
        </Routes>
      </main>
    </>
  );
}
