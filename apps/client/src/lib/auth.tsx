import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as Linking from 'expo-linking';
import type { Session, User } from '@supabase/supabase-js';
import { isConfigured, supabase } from './supabase';

interface AuthState {
  session: Session | null;
  user: User | null;
  loading: boolean;
  signIn(email: string, password: string): Promise<void>;
  signUp(email: string, password: string, fullName: string): Promise<{ needsConfirmation: boolean }>;
  sendMagicLink(email: string): Promise<void>;
  signOut(): Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

/** Translate the most common Supabase auth errors to PT-BR. */
export function authErrorMessage(e: unknown): string {
  const msg = e instanceof Error ? e.message : String(e);
  if (/invalid login credentials/i.test(msg)) return 'E-mail ou senha incorretos.';
  if (/email not confirmed/i.test(msg)) return 'Confirme seu e-mail antes de entrar (verifique sua caixa de entrada).';
  if (/already registered|already been registered/i.test(msg)) return 'Este e-mail já possui cadastro. Faça login.';
  if (/password should be at least/i.test(msg)) return 'A senha deve ter pelo menos 6 caracteres.';
  if (/rate limit|too many/i.test(msg)) return 'Muitas tentativas. Aguarde um instante e tente novamente.';
  if (/network|fetch/i.test(msg)) return 'Sem conexão. Verifique sua internet.';
  return msg || 'Algo deu errado. Tente novamente.';
}

/** Extract tokens/code from a magic-link redirect (implicit flow puts them in the hash). */
export function parseAuthRedirect(url: string): { access_token?: string; refresh_token?: string; code?: string } {
  const out: Record<string, string> = {};
  const parts = [url.split('#')[1] ?? '', (url.split('?')[1] ?? '').split('#')[0]];
  for (const part of parts) {
    for (const kv of part.split('&')) {
      const [k, v] = kv.split('=');
      if (k && v) out[decodeURIComponent(k)] = decodeURIComponent(v);
    }
  }
  return { access_token: out.access_token, refresh_token: out.refresh_token, code: out.code };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(isConfigured);
  const incomingUrl = Linking.useLinkingURL();

  useEffect(() => {
    if (!isConfigured) return;
    let mounted = true;
    supabase.auth
      .getSession()
      .then(({ data }) => mounted && setSession(data.session))
      .finally(() => mounted && setLoading(false));
    const { data: sub } = supabase.auth.onAuthStateChange((_event, s) => setSession(s));
    return () => {
      mounted = false;
      sub.subscription.unsubscribe();
    };
  }, []);

  // Magic-link deep link: regla://auth/callback#access_token=…&refresh_token=…
  useEffect(() => {
    if (!isConfigured || !incomingUrl) return;
    const { access_token, refresh_token, code } = parseAuthRedirect(incomingUrl);
    if (access_token && refresh_token) {
      supabase.auth.setSession({ access_token, refresh_token }).catch(() => undefined);
    } else if (code) {
      supabase.auth.exchangeCodeForSession(code).catch(() => undefined);
    }
  }, [incomingUrl]);

  const signIn = useCallback(async (email: string, password: string) => {
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
    if (error) throw new Error(authErrorMessage(error));
  }, []);

  const signUp = useCallback(async (email: string, password: string, fullName: string) => {
    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
      options: { data: { full_name: fullName.trim() }, emailRedirectTo: Linking.createURL('auth/callback') },
    });
    if (error) throw new Error(authErrorMessage(error));
    return { needsConfirmation: !data.session };
  }, []);

  const sendMagicLink = useCallback(async (email: string) => {
    const { error } = await supabase.auth.signInWithOtp({
      email: email.trim(),
      options: { emailRedirectTo: Linking.createURL('auth/callback') },
    });
    if (error) throw new Error(authErrorMessage(error));
  }, []);

  const signOut = useCallback(async () => {
    await supabase.auth.signOut();
  }, []);

  const value = useMemo<AuthState>(
    () => ({ session, user: session?.user ?? null, loading, signIn, signUp, sendMagicLink, signOut }),
    [session, loading, signIn, signUp, sendMagicLink, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
