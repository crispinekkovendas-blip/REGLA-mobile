import { RefreshControl, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors, radius, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { errorText, useDocuments } from '@/lib/queries';
import { DocumentChecklist, missingRequiredDocs } from '@/components/DocumentChecklist';
import { LoginPrompt } from '@/components/LoginPrompt';
import { Badge, ErrorBox, Loading } from '@/components/ui';

export default function DocumentsScreen() {
  const { user } = useAuth();
  const insets = useSafeAreaInsets();
  const docs = useDocuments();

  if (!user) return <LoginPrompt icon="doc" title="Meus documentos" text="Entre para enviar seus documentos com segurança." />;

  const missing = missingRequiredDocs(docs.data ?? []);

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: colors.bg }}
      contentContainerStyle={{ padding: space.lg, paddingBottom: insets.bottom + space.xxxl }}
      refreshControl={<RefreshControl refreshing={docs.isRefetching} onRefresh={() => docs.refetch()} tintColor={colors.navy} />}
    >
      <View style={{ backgroundColor: colors.navy, borderRadius: radius.lg, padding: space.lg, gap: space.sm, marginBottom: space.lg }}>
        <Text style={[type.h2, { color: colors.white }]}>Documentos em um só lugar</Text>
        <Text style={{ color: 'rgba(255,255,255,0.75)', fontSize: 14, lineHeight: 20 }}>
          Envie uma vez e reutilize em todas as propostas. Arquivos protegidos — acesso apenas da equipe REGLA.
        </Text>
        <View style={{ marginTop: 4 }}>
          {docs.data ? (
            missing.length ? <Badge label={`${missing.length} obrigatório${missing.length > 1 ? 's' : ''} pendente${missing.length > 1 ? 's' : ''}`} tone="accent" /> : <Badge label="Tudo certo" tone="success" />
          ) : null}
        </View>
      </View>
      {docs.error ? <ErrorBox message={errorText(docs.error)} onRetry={() => docs.refetch()} /> : null}
      {docs.isLoading ? <Loading /> : <DocumentChecklist documents={docs.data ?? []} />}
      <Text style={[type.small, { marginTop: space.lg }]}>Formatos aceitos: PDF, JPG, PNG · até 10 MB por arquivo.</Text>
    </ScrollView>
  );
}
