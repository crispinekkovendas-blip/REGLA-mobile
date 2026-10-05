import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { colors, radius, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { Button, Field } from '@/components/ui';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function SignupScreen() {
  const { signUp } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [needsConfirm, setNeedsConfirm] = useState(false);

  const onSubmit = async () => {
    const e: Record<string, string> = {};
    if (name.trim().length < 3) e.name = 'Informe seu nome completo';
    if (!EMAIL_RE.test(email.trim())) e.email = 'E-mail inválido';
    if (password.length < 6) e.password = 'Use pelo menos 6 caracteres';
    if (confirm !== password) e.confirm = 'As senhas não conferem';
    setErrors(e);
    if (Object.keys(e).length) return;
    setLoading(true);
    try {
      const res = await signUp(email, password, name);
      if (res.needsConfirmation) setNeedsConfirm(true);
      else router.canGoBack() ? router.back() : router.replace('/');
    } catch (err) {
      setErrors({ form: (err as Error).message });
    } finally {
      setLoading(false);
    }
  };

  if (needsConfirm) {
    return (
      <View style={[styles.wrap, { flex: 1, justifyContent: 'center' }]}>
        <Text style={type.display}>Confira seu e-mail</Text>
        <Text style={[type.body, { color: colors.muted, marginVertical: space.lg }]}>
          Enviamos um link de confirmação para {email.trim()}. Depois de confirmar, é só entrar com sua senha.
        </Text>
        <Button variant="accent" title="Ir para o login" onPress={() => router.replace('/auth/login')} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bg }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.wrap} keyboardShouldPersistTaps="handled">
        <Text style={styles.brand}>REGLA</Text>
        <Text style={type.display}>Crie sua conta</Text>
        <Text style={[type.body, { color: colors.muted, marginBottom: space.xl }]}>Grátis. Seu cadastro vale para todas as propostas.</Text>
        <Field label="Nome completo" value={name} onChangeText={setName} autoComplete="name" error={errors.name} testID="signup-name" />
        <Field label="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" error={errors.email} testID="signup-email" />
        <Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry autoComplete="new-password" error={errors.password} hint="Mínimo de 6 caracteres" testID="signup-password" />
        <Field label="Confirmar senha" value={confirm} onChangeText={setConfirm} secureTextEntry error={errors.confirm} onSubmitEditing={onSubmit} testID="signup-confirm" />
        {errors.form ? <Text style={styles.formErr}>{errors.form}</Text> : null}
        <Button variant="accent" title="Criar conta" loading={loading} onPress={onSubmit} testID="btn-signup" />
        <Text style={styles.footer}>
          Já tem conta?{' '}
          <Text style={styles.link} onPress={() => router.replace('/auth/login')}>
            Entrar
          </Text>
        </Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  wrap: { padding: space.xl, paddingTop: space.xxl, gap: 2, maxWidth: 520, width: '100%', alignSelf: 'center', backgroundColor: colors.bg },
  brand: { color: colors.coral, fontWeight: '900', letterSpacing: 4, fontSize: 13, marginBottom: space.md },
  formErr: { color: colors.danger, fontWeight: '600', marginBottom: space.md, backgroundColor: colors.dangerSoft, padding: space.md, borderRadius: radius.md },
  footer: { textAlign: 'center', marginTop: space.xxl, color: colors.muted, fontSize: 15 },
  link: { color: colors.coral, fontWeight: '800' },
});
