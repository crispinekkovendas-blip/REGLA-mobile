import { useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import {
  APPLICATION_STATUS_LABEL, GUARANTEE_LABEL, formatPrice, listingRef, type ApplicationWithListing,
} from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { errorText, useApplications, useWithdrawApplication } from '@/lib/queries';
import { formatDateBR } from '@/lib/slots';
import { LoginPrompt } from '@/components/LoginPrompt';
import { ScreenTitle } from '@/components/ScreenTitle';
import { STATUS_TONE, StatusTimeline } from '@/components/StatusTimeline';
import { Badge, Button, EmptyState, ErrorBox, Loading } from '@/components/ui';

const OPEN = new Set(['submitted', 'under_review', 'docs_requested']);

function ApplicationCard({ a }: { a: ApplicationWithListing }) {
  const withdraw = useWithdrawApplication();
  const [confirming, setConfirming] = useState(false);
  const currency = a.listings?.currency ?? 'BRL';
  return (
    <View style={styles.card} testID={`application-${a.id}`}>
      <Pressable onPress={() => router.push(`/listing/${a.listing_id}`)} accessibilityRole="button">
        <View style={styles.head}>
          <Text style={styles.ref}>{listingRef(a.listing_id)}</Text>
          <Badge label={APPLICATION_STATUS_LABEL[a.status]} tone={STATUS_TONE[a.status]} />
        </View>
        <Text style={type.h3} numberOfLines={1}>
          {a.listings?.title ?? 'Imóvel'}
        </Text>
        {a.listings ? <Text style={type.small}>{[a.listings.neighborhood, a.listings.city].filter(Boolean).join(' · ')}</Text> : null}
      </Pressable>

      <View style={styles.facts}>
        <View style={styles.fact}>
          <Text style={styles.factLabel}>{a.intent === 'rent' ? 'Aluguel proposto' : 'Oferta'}</Text>
          <Text style={styles.factValue}>
            {formatPrice(a.offered_price, currency)}
            {a.intent === 'rent' ? <Text style={type.small}>/mês</Text> : null}
          </Text>
        </View>
        <View style={styles.fact}>
          <Text style={styles.factLabel}>{a.intent === 'rent' ? 'Garantia' : 'Enviada em'}</Text>
          <Text style={styles.factValue}>
            {a.intent === 'rent' ? (a.guarantee_type ? GUARANTEE_LABEL[a.guarantee_type] : '—') : formatDateBR(a.created_at)}
          </Text>
        </View>
      </View>

      <StatusTimeline status={a.status} />

      {a.reviewer_note ? (
        <View style={styles.note}>
          <Text style={styles.noteLabel}>Mensagem do corretor</Text>
          <Text style={type.body}>{a.reviewer_note}</Text>
        </View>
      ) : null}

      {a.status === 'docs_requested' ? (
        <Button title="Enviar documentos" variant="accent" icon="upload" style={{ marginTop: space.md }} onPress={() => router.push('/profile/documents')} />
      ) : null}

      {OPEN.has(a.status) ? (
        <View style={{ marginTop: space.md }}>
          {withdraw.error ? <Text style={styles.err}>{errorText(withdraw.error)}</Text> : null}
          {confirming ? (
            <View style={{ flexDirection: 'row', gap: space.sm }}>
              <Button small variant="outline" title="Manter" onPress={() => setConfirming(false)} style={{ flex: 1 }} />
              <Button
                small
                variant="danger"
                title="Confirmar cancelamento"
                loading={withdraw.isPending}
                onPress={() => withdraw.mutate(a.id, { onSettled: () => setConfirming(false) })}
                style={{ flex: 1.6 }}
              />
            </View>
          ) : (
            <Button small variant="ghost" title="Cancelar proposta" onPress={() => setConfirming(true)} />
          )}
        </View>
      ) : null}
    </View>
  );
}

export default function ApplicationsScreen() {
  const { user } = useAuth();
  const apps = useApplications();

  if (!user) return <LoginPrompt icon="doc" title="Suas propostas" text="Entre para enviar propostas e acompanhar cada etapa da análise." />;

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: colors.bg }}
      contentContainerStyle={{ paddingBottom: space.xxxl }}
      refreshControl={<RefreshControl refreshing={apps.isRefetching} onRefresh={() => apps.refetch()} tintColor={colors.navy} />}
    >
      <ScreenTitle title="Propostas" subtitle="Acompanhe o andamento de cada uma" />
      <View style={{ paddingHorizontal: space.lg }}>
        {apps.error ? <ErrorBox message={errorText(apps.error)} onRetry={() => apps.refetch()} /> : null}
        {apps.isLoading ? (
          <Loading />
        ) : !apps.data?.length ? (
          <EmptyState
            icon="doc"
            title="Nenhuma proposta enviada"
            text="Quando encontrar o imóvel ideal, toque em “Fazer proposta”. É rápido e sem papelada."
            action={<Button title="Buscar imóveis" variant="accent" onPress={() => router.navigate('/')} />}
          />
        ) : (
          apps.data.map((a) => <ApplicationCard key={a.id} a={a} />)
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  card: { backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, marginBottom: space.lg, ...shadow },
  head: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: space.sm },
  ref: { fontSize: 12, fontWeight: '800', color: colors.muted, letterSpacing: 0.6 },
  facts: { flexDirection: 'row', gap: space.md, marginTop: space.md },
  fact: { flex: 1, backgroundColor: colors.navySoft, borderRadius: radius.md, padding: space.md },
  factLabel: { fontSize: 11, fontWeight: '700', color: colors.muted, textTransform: 'uppercase', letterSpacing: 0.5 },
  factValue: { fontSize: 16, fontWeight: '800', color: colors.navy, marginTop: 2 },
  note: { marginTop: space.md, backgroundColor: colors.coralSoft, borderRadius: radius.md, padding: space.md, gap: 4 },
  noteLabel: { fontSize: 12, fontWeight: '800', color: colors.coralDark },
  err: { color: colors.danger, fontSize: 13, marginBottom: space.sm },
});
