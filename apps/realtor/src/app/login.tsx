import { useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, Text, TextInput, View } from 'react-native';
import { useAuth } from '../lib/auth';
import { Brand } from '../components/GateScreens';
import { Button, InlineError } from '../components/ui';
import { colors, radius, space } from '../theme';

export default function LoginScreen() {
  const { signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!email.trim() || !password) { setError('Informe e-mail e senha.'); return; }
    setBusy(true);
    setError(null);
    try {
      await signIn(email, password);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Falha ao entrar.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.shell} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={styles.inner}>
        <Brand />
        <Text style={styles.title}>Painel do corretor</Text>
        <Text style={styles.sub}>Leads, propostas, visitas e imóveis em um só lugar.</Text>

        <View style={styles.form}>
          <Text style={styles.label}>E-mail</Text>
          <TextInput
            testID="login-email"
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            autoComplete="email"
            keyboardType="email-address"
            placeholder="voce@imoveisregla.com.br"
            placeholderTextColor="#6F819C"
            style={styles.input}
          />
          <Text style={styles.label}>Senha</Text>
          <TextInput
            testID="login-password"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="password"
            placeholder="••••••••"
            placeholderTextColor="#6F819C"
            style={styles.input}
            onSubmitEditing={submit}
            returnKeyType="go"
          />
          <InlineError message={error} />
          <Button label="Entrar" variant="coral" loading={busy} onPress={submit} style={{ marginTop: space.sm }} testID="login-submit" />
        </View>
        <Text style={styles.foot}>Acesso exclusivo para corretores cadastrados pela REGLA.</Text>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  shell: { flex: 1, backgroundColor: colors.navy, justifyContent: 'center', padding: space.xl },
  inner: { width: '100%', maxWidth: 420, alignSelf: 'center' },
  title: { color: colors.white, fontSize: 28, fontWeight: '800', letterSpacing: -0.6 },
  sub: { color: colors.onNavyMuted, fontSize: 15, marginTop: 4 },
  form: { marginTop: space.xl, gap: space.sm },
  label: { color: colors.onNavyMuted, fontSize: 11, fontWeight: '700', letterSpacing: 0.8, textTransform: 'uppercase', marginTop: space.sm },
  input: {
    height: 48, borderRadius: radius.md, paddingHorizontal: space.md, fontSize: 16,
    backgroundColor: colors.navyDeep, color: colors.white, borderWidth: 1, borderColor: colors.navyMid,
  },
  foot: { color: colors.onNavyMuted, fontSize: 12, marginTop: space.xl, opacity: 0.8 },
});
