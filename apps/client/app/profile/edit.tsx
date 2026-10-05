import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors, radius, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { errorText, useProfile, useSaveProfile } from '@/lib/queries';
import { profileToForm } from '@/lib/forms';
import { ProfileEditor } from '@/components/ProfileEditor';
import { LoginPrompt } from '@/components/LoginPrompt';
import { ErrorBox, Loading } from '@/components/ui';

export default function EditProfileScreen() {
  const { user } = useAuth();
  const insets = useSafeAreaInsets();
  const profile = useProfile();
  const save = useSaveProfile();
  const [saved, setSaved] = useState(false);

  if (!user) return <LoginPrompt icon="user" title="Meu cadastro" text="Entre para preencher seu cadastro." />;
  if (profile.isLoading) return <Loading />;

  const initial = profileToForm(profile.data ?? null, user.email ?? '', (user.user_metadata?.full_name as string | undefined) ?? '');

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bg }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={{ padding: space.lg, paddingBottom: insets.bottom + space.xxxl }} keyboardShouldPersistTaps="handled">
        <Text style={[type.body, { color: colors.muted }]}>
          Preencha uma vez e use em todas as propostas. Seus dados só são compartilhados com o proprietário quando você envia uma proposta.
        </Text>
        {profile.error ? <ErrorBox message={errorText(profile.error)} onRetry={() => profile.refetch()} /> : null}
        <ProfileEditor
          initial={initial}
          saving={save.isPending}
          onSave={(input) =>
            save.mutateAsync(input).then(
              () => {
                setSaved(true);
                setTimeout(() => (router.canGoBack() ? router.back() : router.replace('/perfil')), 700);
              },
              () => undefined,
            )
          }
        />
        {save.error ? <ErrorBox message={errorText(save.error)} /> : null}
        {saved ? (
          <View style={{ backgroundColor: colors.successSoft, padding: space.md, borderRadius: radius.md, marginTop: space.md }}>
            <Text style={{ color: colors.success, fontWeight: '700' }}>Cadastro salvo!</Text>
          </View>
        ) : null}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
