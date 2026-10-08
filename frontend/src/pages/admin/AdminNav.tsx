import { NavLink, useNavigate } from 'react-router-dom';
import { adminApi, getSession, setSession } from '../../api/client';

export default function AdminNav() {
  const navigate = useNavigate();
  const session = getSession();

  async function logout() {
    try {
      await adminApi.logout();
    } finally {
      setSession(null);
      navigate('/admin/login');
    }
  }

  return (
    <nav className="admin-nav">
      <NavLink to="/admin/companies">Companies</NavLink>
      <NavLink to="/admin/appointments">Appointments</NavLink>
      <span className="muted">{session?.email}</span>
      <button className="secondary" onClick={logout}>Log out</button>
    </nav>
  );
}
