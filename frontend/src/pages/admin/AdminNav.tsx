import { NavLink, useNavigate } from 'react-router-dom';
import { adminApi, getSession, setSession } from '../../api/client';
import { t } from '../../i18n/sr';

export default function AdminNav() {
  const navigate = useNavigate();
  const session = getSession();

  async function logout() {
    try {
      await adminApi.logout();
    } catch {
      /* logged out locally anyway */
    } finally {
      setSession(null);
      navigate('/admin/login');
    }
  }

  return (
    <nav className="admin-nav">
      <NavLink to="/admin/appointments" end>
        {t.nav.appointments}
      </NavLink>
      <NavLink to="/admin/calendar">{t.nav.calendar}</NavLink>
      <NavLink to="/admin/appointments/new">{t.nav.newAppointment}</NavLink>
      <NavLink to="/admin/companies">
        {t.nav.companies}
      </NavLink>
      <span className="muted">{session?.email}</span>
      <button className="secondary" onClick={logout}>
        {t.nav.logout}
      </button>
    </nav>
  );
}
