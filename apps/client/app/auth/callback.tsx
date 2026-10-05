import { useEffect } from 'react';
import { router } from 'expo-router';
import { useAuth } from '@/lib/auth';
import { Loading } from '@/components/ui';

/** Magic-link / e-mail confirmation landing. AuthProvider consumes the tokens; we just wait for the session. */
export default function AuthCallback() {
  const { session, loading } = useAuth();
  useEffect(() => {
    if (session) router.replace('/');
    else if (!loading) {
      const t = setTimeout(() => router.replace('/auth/login'), 4000);
      return () => clearTimeout(t);
    }
  }, [session, loading]);
  return <Loading label="Entrando…" />;
}
