import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { colors, radius, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { Button, Field } from '@/components/ui';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginScreen() {
  const { signIn, sendMagicLink } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string; form?: string }>({});
  const [loading, setLoading] = useState<'password' | 'magic' | null>(null);
  const [magicSent, setMagicSent] = useState(false);

  const close = () => (router.canGoBack() ? router.back() : router.replace('/'));

  const onLogin = async () => {
    const e: typeof errors = {};
    if (!EMAIL_RE.test(email.trim())) e.email = 'E-mail inválido';
    if (password.length < 6) e.password = 'A senha tem pelo menos 6 caracteres';
    setErrors(e);
    if (Object.keys(e).length) return;
    setLoading('password');
    try {
      await signIn(email, password);
      close();
    } catch (err) {
      setErrors({ form: (err as Error).message });
    } finally {
      setLoading(null);
    }
  };

  const onMagic = async () => {
    if (!EMAIL_RE.test(email.trim())) {
      setErrors({ email: 'Informe seu e-mail para receber o link' });
      return;
    }
    setLoading('magic');
    setErrors({});
    try {
      await sendMagicLink(email);
      setMagicSent(true);
    } catch (err) {
      setErrors({ form: (err as Error).message });
    } finally {
      setLoading(null);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bg }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.wrap} keyboardShouldPersistTaps="handled">
        <Text style={styles.brand}>REGLA</Text>
        <Text style={type.display}>Bem-vindo de volta</Text>
        <Text style={[type.body, { color: colors.muted, marginBottom: space.xl }]}>Entre para favoritar, agendar visitas e enviar propostas.</Text>

        <Field label="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" error={errors.email} placeholder="voce@email.com" testID="login-email" />
        <Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry autoComplete="password" error={errors.password} placeholder="••••••" onSubmitEditing={onLogin} testID="login-password" />
        {errors.form ? <Text style={styles.formErr}>{errors.form}</Text> : null}

        <Button variant="accent" title="Entrar" loading={loading === 'password'} onPress={onLogin} testID="btn-login" />

        <View style={styles.orRow}>
          <View style={styles.orLine} />
          <Text style={type.small}>ou</Text>
          <View style={styles.orLine} />
        </View>

        {magicSent ? (
          <View style={styles.sent}>
            <Text style={{ fontWeight: '700', color: colors.success }}>Link enviado!</Text>
            <Text style={type.small}>Abra o e-mail em {email.trim()} neste aparelho para entrar.</Text>
          </View>
        ) : (
          <Button variant="outline" title="Receber link de acesso por e-mail" loading={loading === 'magic'} onPress={onMagic} />
        )}

        <Text style={styles.footer}>
          Ainda não tem conta?{' '}
          <Text style={styles.link} onPress={() => router.replace('/auth/signup')}>
            Criar conta
          </Text>
        </Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  wrap: { padding: space.xl, paddingTop: space.xxl, gap: 2, maxWidth: 520, width: '100%', alignSelf: 'center' },
  brand: { color: colors.coral, fontWeight: '900', letterSpacing: 4, fontSize: 13, marginBottom: space.md },
  formErr: { color: colors.danger, fontWeight: '600', marginBottom: space.md, backgroundColor: colors.dangerSoft, padding: space.md, borderRadius: radius.md },
  orRow: { flexDirection: 'row', alignItems: 'center', gap: space.md, marginVertical: space.lg },
  orLine: { flex: 1, height: StyleSheet.hairlineWidth, backgroundColor: colors.line },
  sent: { backgroundColor: colors.successSoft, padding: space.lg, borderRadius: radius.md, gap: 4 },
  footer: { textAlign: 'center', marginTop: space.xxl, color: colors.muted, fontSize: 15 },
  link: { color: colors.coral, fontWeight: '800' },
});
