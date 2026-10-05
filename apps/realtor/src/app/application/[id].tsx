import { useState } from 'react';
import { Linking, StyleSheet, Text, View } from 'react-native';
import { Stack, router, useLocalSearchParams } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';
import { useQuery } from '@tanstack/react-query';
import {
  DOCUMENT_KIND_LABEL, EMPLOYMENT_LABEL, GUARANTEE_LABEL, affordability, fetchApplicationForReview,
  fetchDocuments, formatPrice, listingRef, signedDocumentUrl, waToClient, type ClientDocument,
} from '@regla/shared';
import { getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { useAuth } from '../../lib/auth';
import { ApplicationReview } from '../../components/ApplicationReview';
import {
  Avatar, Button, Card, EmptyState, ErrorState, Field, InlineError, Loading, Pill, Row, Screen, SectionHeader,
} from '../../components/ui';
import { INTENT_LABEL, formatBytes, maskCpfPartial } from '../../utils/labels';
import { formatIsoDate, relativeTime } from '../../utils/agenda';
import { colors, radius, space, tones, type, type Tone } from '../../theme';

const AFFORD: Record<ReturnType<typeof affordability>, { label: string; hint: string; tone: Tone }> = {
  ok: { label: 'Renda compatível', hint: 'Aluguel até 30% da renda', tone: 'ok' },
  tight: { label: 'Renda no limite', hint: 'Aluguel entre 30% e 40% da renda', tone: 'warn' },
  over: { label: 'Renda insuficiente', hint: 'Aluguel acima de 40% da renda', tone: 'danger' },
  unknown: { label: 'Renda não informada', hint: 'Peça comprovante de renda', tone: 'neutral' },
};

export default function ApplicationDetailScreen() {
  const { id: raw } = useLocalSearchParams<{ id: string }>();
  const id = Number(raw);
  const { userId } = useAuth();
  const [docError, setDocError] = useState<string | null>(null);
  const [opening, setOpening] = useState<number | null>(null);

  const q = useQuery({ queryKey: qk.application(id), queryFn: () => fetchApplicationForReview(getSupabase(), id), enabled: Number.isFinite(id) });
  const clientId = q.data?.user_id;
  const docs = useQuery({
    queryKey: qk.documents(clientId ?? ''),
    queryFn: () => fetchDocuments(getSupabase(), clientId!),
    enabled: !!clientId,
  });

  if (q.isLoading) return <Screen><Loading /></Screen>;
  if (q.error || !q.data) return <Screen><ErrorState error={q.error ?? 'Proposta não encontrada'} onRetry={() => q.refetch()} /></Screen>;

  const a = q.data;
  const p = a.client_profiles;
  const listing = a.listings;
  const currency = listing?.currency ?? 'BRL';
  const diff = listing ? a.offered_price - listing.price : 0;
  const diffPct = listing && listing.price ? (diff / listing.price) * 100 : 0;
  const aff = a.intent === 'rent' ? affordability(a.offered_price, p?.monthly_income ?? null) : null;
  const ratio = aff && p?.monthly_income ? Math.round((a.offered_price / p.monthly_income) * 100) : null;

  // Documents tied to this proposta first, then the client's general documents.
  const allDocs = docs.data ?? [];
  const appDocs = allDocs.filter((d) => d.application_id === a.id);
  const otherDocs = allDocs.filter((d) => d.application_id !== a.id);

  const openDoc = async (d: ClientDocument) => {
    setDocError(null);
    setOpening(d.id);
    try {
      const url = await signedDocumentUrl(getSupabase(), d.storage_path, 300);
      await WebBrowser.openBrowserAsync(url);
    } catch (e) {
      setDocError(e instanceof Error ? e.message : 'Falha ao abrir o documento.');
    } finally {
      setOpening(null);
    }
  };

  const firstName = p?.full_name.split(' ')[0] ?? '';
  const waText = `Olá ${firstName}, aqui é da REGLA Imóveis sobre sua proposta${listing ? ` para "${listing.title}" (ref. ${listingRef(listing.id)})` : ''}.`;

  return (
    <Screen refreshing={q.isRefetching || docs.isRefetching} onRefresh={() => { q.refetch(); docs.refetch(); }}>
      <Stack.Screen options={{ title: `Proposta #${a.id}` }} />

      {/* Applicant */}
      <Card testID="applicant-card">
        <Row gap={space.md}>
          <Avatar name={p?.full_name ?? '?'} size={48} />
          <View style={{ flex: 1 }}>
            <Text style={type.title} numberOfLines={1}>{p?.full_name ?? 'Cliente sem cadastro'}</Text>
            <Text style={type.small}>Enviada {relativeTime(a.created_at)}</Text>
          </View>
        </Row>
        {p ? (
          <>
            <View style={styles.fields}>
              <Field label="CPF" value={maskCpfPartial(p.cpf)} mono />
              <Field label="Telefone" value={p.phone || '—'} mono />
              <Field label="E-mail" value={p.email} />
              <Field label="Ocupação" value={p.occupation || '—'} />
              <Field label="Vínculo" value={p.employment_type ? EMPLOYMENT_LABEL[p.employment_type] : '—'} />
              <Field label="Renda mensal" value={p.monthly_income != null ? formatPrice(p.monthly_income, 'BRL') : '—'} mono />
              <Field label="Moradores" value={String(p.residents)} mono />
              <Field label="Pets" value={p.has_pets ? 'Sim' : 'Não'} />
            </View>
            <View style={styles.contactRow}>
              <Button small flex variant="coral" label="WhatsApp" disabled={!p.phone} onPress={() => Linking.openURL(waToClient(p.phone, waText))} testID="wa-client" />
              <Button small flex variant="secondary" label="Ligar" disabled={!p.phone} onPress={() => Linking.openURL(`tel:${p.phone.replace(/[^\d+]/g, '')}`)} />
              <Button small flex variant="secondary" label="E-mail" onPress={() => Linking.openURL(`mailto:${p.email}`)} />
            </View>
          </>
        ) : <Text style={[type.small, { marginTop: space.sm }]}>O cliente ainda não preencheu o cadastro.</Text>}
      </Card>

      {/* Proposal */}
      <Card>
        <Text style={[type.over, { marginBottom: space.sm }]}>Proposta · {INTENT_LABEL[a.intent]}</Text>
        {listing ? (
          <Card style={styles.linked} onPress={() => router.push({ pathname: '/listing/[id]', params: { id: String(listing.id) } })}>
            <Text style={type.mono}>{listingRef(listing.id)}</Text>
            <View style={{ flex: 1 }}>
              <Text style={type.h2} numberOfLines={1}>{listing.title}</Text>
              <Text style={type.small} numberOfLines={1}>{listing.neighborhood}, {listing.city}</Text>
            </View>
            <Text style={{ color: colors.coral, fontWeight: '800' }}>›</Text>
          </Card>
        ) : null}
        <View style={styles.priceRow}>
          <View style={{ flex: 1 }}>
            <Text style={type.over}>Oferta</Text>
            <Text style={styles.bigPrice}>{formatPrice(a.offered_price, currency)}</Text>
          </View>
          {listing ? (
            <View style={{ flex: 1, alignItems: 'flex-end' }}>
              <Text style={type.over}>Anunciado</Text>
              <Text style={styles.listPrice}>{formatPrice(listing.price, currency)}</Text>
              <Text style={[styles.diff, { color: diff < 0 ? colors.danger : diff > 0 ? '#0F9D63' : colors.muted }]}>
                {diff === 0 ? 'Valor pedido' : `${diff > 0 ? '+' : ''}${diffPct.toFixed(1).replace('.', ',')}% (${diff > 0 ? '+' : '−'}${formatPrice(Math.abs(diff), currency)})`}
              </Text>
            </View>
          ) : null}
        </View>
        <View style={styles.fields}>
          <Field label="Garantia" value={a.guarantee_type ? GUARANTEE_LABEL[a.guarantee_type] : '—'} />
          <Field label="Mudança" value={formatIsoDate(a.move_in_date)} mono />
        </View>
        {a.message ? (
          <View style={styles.quote}><Text style={type.body}>“{a.message}”</Text></View>
        ) : null}
        {aff ? (
          <View style={[styles.afford, { backgroundColor: tones[AFFORD[aff].tone].bg }]} testID="affordability">
            <View style={[styles.affordBar, { backgroundColor: tones[AFFORD[aff].tone].dot }]} />
            <View style={{ flex: 1 }}>
              <Text style={[styles.affordTitle, { color: tones[AFFORD[aff].tone].fg }]}>{AFFORD[aff].label}</Text>
              <Text style={[type.small, { color: tones[AFFORD[aff].tone].fg }]}>{AFFORD[aff].hint}</Text>
            </View>
            {ratio != null ? <Text style={[styles.affordPct, { color: tones[AFFORD[aff].tone].fg }]}>{ratio}%</Text> : null}
          </View>
        ) : (
          <Pill label="Compra — análise de crédito/financiamento" tone="info" />
        )}
      </Card>

      {/* Documents */}
      <SectionHeader title={`Documentos (${allDocs.length})`} />
      <Card style={{ paddingVertical: space.sm }}>
        {docs.isLoading ? <Loading /> : docs.error ? <ErrorState error={docs.error} onRetry={() => docs.refetch()} /> : allDocs.length ? (
          [...appDocs, ...otherDocs].map((d, i) => (
            <View key={d.id} style={[styles.doc, i > 0 && styles.docBorder]}>
              <View style={styles.docIcon}><Text style={styles.docExt}>{(d.filename.split('.').pop() ?? 'doc').slice(0, 4).toUpperCase()}</Text></View>
              <View style={{ flex: 1 }}>
                <Text style={type.h2} numberOfLines={1}>{DOCUMENT_KIND_LABEL[d.kind]}</Text>
                <Text style={type.small} numberOfLines={1}>
                  {d.filename} · {formatBytes(d.size_bytes)}{d.application_id === a.id ? ' · desta proposta' : ''}
                </Text>
              </View>
              <Button small variant="secondary" label="Abrir" loading={opening === d.id} onPress={() => openDoc(d)} testID={`doc-open-${d.id}`} />
            </View>
          ))
        ) : <EmptyState title="Nenhum documento enviado" hint='Use "Pedir documentos" para solicitar ao cliente.' />}
        <InlineError message={docError} />
      </Card>

      {/* Review */}
      <ApplicationReview
        applicationId={a.id}
        currentStatus={a.status}
        currentNote={a.reviewer_note}
        reviewerId={userId ?? ''}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  fields: { flexDirection: 'row', flexWrap: 'wrap', columnGap: space.md, marginTop: space.md },
  contactRow: { flexDirection: 'row', gap: space.sm, marginTop: space.md },
  linked: { flexDirection: 'row', alignItems: 'center', gap: space.sm, padding: space.md, backgroundColor: colors.navySoft, marginBottom: space.md },
  priceRow: { flexDirection: 'row', alignItems: 'flex-end' },
  bigPrice: { fontSize: 24, fontWeight: '800', color: colors.navy, letterSpacing: -0.4, ...type.num },
  listPrice: { fontSize: 15, fontWeight: '700', color: colors.muted, ...type.num },
  diff: { fontSize: 12, fontWeight: '700', ...type.num },
  quote: { borderLeftWidth: 3, borderLeftColor: colors.line, paddingLeft: space.md, marginVertical: space.md },
  afford: { flexDirection: 'row', alignItems: 'center', gap: space.md, borderRadius: radius.md, padding: space.md, marginTop: space.sm, overflow: 'hidden' },
  affordBar: { width: 4, alignSelf: 'stretch', borderRadius: 2 },
  affordTitle: { fontWeight: '800', fontSize: 14 },
  affordPct: { fontSize: 22, fontWeight: '800', ...type.num },
  doc: { flexDirection: 'row', alignItems: 'center', gap: space.md, paddingVertical: space.sm },
  docBorder: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.line },
  docIcon: { width: 38, height: 44, borderRadius: radius.sm, backgroundColor: colors.navySoft, borderWidth: 1, borderColor: colors.line, alignItems: 'center', justifyContent: 'center' },
  docExt: { fontSize: 9, fontWeight: '800', color: colors.navy },
});
