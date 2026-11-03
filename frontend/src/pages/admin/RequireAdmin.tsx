import { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { getSession } from '../../api/client';
import AdminNav from './AdminNav';

export default function RequireAdmin({ children }: { children: ReactNode }) {
  if (!getSession()) return <Navigate to="/admin/login" replace />;
  return (
    <>
      <AdminNav />
      {children}
    </>
  );
}
