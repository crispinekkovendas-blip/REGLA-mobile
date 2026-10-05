import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { Session } from '@supabase/supabase-js';
import { isRealtor } from '@regla/shared';
import { supabase } from './supabase';

type RealtorState = 'unknown' | 'checking' | 'yes' | 'no';

interface AuthValue {
  ready: boolean;
  session: Session | null;
  userId: string | null;
  email: string | null;
  realtor: RealtorState;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [session, setSession] = useState<Session | null>(null);
  const [realtor, setRealtor] = useState<RealtorState>('unknown');

  useEffect(() => {
    if (!supabase) { setReady(true); return; }
    let alive = true;
    supabase.auth.getSession()
      .then(({ data }) => { if (alive) setSession(data.session); })
      .catch(() => undefined)
      .finally(() => { if (alive) setReady(true); });
    const { data: sub } = supabase.auth.onAuthStateChange((_event, s) => setSession(s));
    return () => { alive = false; sub.subscription.unsubscribe(); };
  }, []);

  const uid = session?.user.id ?? null;
  useEffect(() => {
    if (!supabase || !uid) { setRealtor('unknown'); return; }
    let alive = true;
    setRealtor('checking');
    isRealtor(supabase).then((ok) => { if (alive) setRealtor(ok ? 'yes' : 'no'); });
    return () => { alive = false; };
  }, [uid]);

  const signIn = useCallback(async (email: string, password: string) => {
    if (!supabase) throw new Error('Supabase não configurado.');
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
    if (error) {
      throw new Error(/invalid login/i.test(error.message) ? 'E-mail ou senha incorretos.' : error.message);
    }
  }, []);

  const signOut = useCallback(async () => {
    await supabase?.auth.signOut();
  }, []);

  const value = useMemo<AuthValue>(() => ({
    ready, session, userId: uid, email: session?.user.email ?? null, realtor, signIn, signOut,
  }), [ready, session, uid, realtor, signIn, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth fora do AuthProvider');
  return ctx;
}
