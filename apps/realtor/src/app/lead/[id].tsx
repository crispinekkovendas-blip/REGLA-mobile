import { useState } from 'react';
import { Linking, StyleSheet, Text, TextInput, View } from 'react-native';
import { Stack, router, useLocalSearchParams } from 'expo-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  STAGE_LABEL, addNote, fetchLead, fetchNotes, listingRef, updateLead, waToClient,
  type Inquiry, type InquiryWithListing,
} from '@regla/shared';
import { getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { useAuth } from '../../lib/auth';
import { findLeadPhone } from '../../lib/leadContact';
import {
  Avatar, Button, Card, ErrorState, InlineError, Loading, Pill, Row, Screen, SectionHeader, Segmented,
} from '../../components/ui';
import { STAGES } from '../../utils/pipeline';
import { INTENT_LABEL, PRIORITIES, PRIORITY_LABEL, STAGE_TONE } from '../../utils/labels';
import { relativeTime } from '../../utils/agenda';
import { colors, radius, space, type } from '../../theme';

type LeadPatch = Partial<Pick<Inquiry, 'stage' | 'priority' | 'read'>>;

export default function LeadDetailScreen() {
  const { id: raw } = useLocalSearchParams<{ id: string }>();
  const id = Number(raw);
  const qc = useQueryClient();
  const { userId } = useAuth();
  const [note, setNote] = useState('');

  const lead = useQuery({ queryKey: qk.lead(id), queryFn: () => fetchLead(getSupabase(), id), enabled: Number.isFinite(id) });
  const notes = useQuery({ queryKey: qk.notes(id), queryFn: () => fetchNotes(getSupabase(), id), enabled: Number.isFinite(id) });
  const phone = useQuery({
    queryKey: qk.leadPhone(id),
    queryFn: () => findLeadPhone(getSupabase(), id, lead.data!.email),
    enabled: !!lead.data,
    staleTime: 5 * 60_000,
  });

  const patch = useMutation({
    mutationFn: (p: LeadPatch) => updateLead(getSupabase(), id, p),
    onMutate: async (p) => {
      // Optimistic: reflect the selector change immediately.
      await qc.cancelQueries({ queryKey: qk.lead(id) });
      const prev = qc.getQueryData<InquiryWithListing>(qk.lead(id));
      if (prev) qc.setQueryData(qk.lead(id), { ...prev, ...p });
      return { prev };
    },
    onError: (_e, _p, ctx) => { if (ctx?.prev) qc.setQueryData(qk.lead(id), ctx.prev); },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: qk.lead(id) });
      qc.invalidateQueries({ queryKey: qk.leads });
      qc.invalidateQueries({ queryKey: qk.stats });
    },
  });

  const add = useMutation({
    mutationFn: (body: string) => addNote(getSupabase(), id, body, userId ?? ''),
    onSuccess: () => {
      setNote('');
      qc.invalidateQueries({ queryKey: qk.notes(id) });
      qc.invalidateQueries({ queryKey: qk.leads });
    },
  });

  const refresh = () => { lead.refetch(); notes.refetch(); phone.refetch(); };

  if (lead.isLoading) return <Screen><Loading /></Screen>;
  if (lead.error || !lead.data) return <Screen><ErrorState error={lead.error ?? 'Lead não encontrado'} onRetry={() => lead.refetch()} /></Screen>;
  const l = lead.data;
  const tel = phone.data ?? null;
  const firstName = l.name.split(' ')[0] ?? l.name;
  const waText = `Olá ${firstName}, aqui é da REGLA Imóveis${l.listings ? ` sobre o imóvel "${l.listings.title}" (ref. ${listingRef(l.listings.id)})` : ''}. Podemos conversar?`;

  return (
    <Screen refreshing={lead.isRefetching || notes.isRefetching} onRefresh={refresh}>
      <Stack.Screen options={{ title: l.name }} />

      <Card>
        <Row gap={space.md}>
          <Avatar name={l.name} size={48} />
          <View style={{ flex: 1 }}>
            <Text style={type.title} numberOfLines={1}>{l.name}</Text>
            <Text style={type.small} selectable>{l.email}</Text>
            {tel ? <Text style={type.small} selectable>{tel}</Text> : null}
          </View>
          <Pill label={STAGE_LABEL[l.stage]} tone={STAGE_TONE[l.stage]} />
        </Row>
        <View style={styles.contactRow}>
          <Button small flex variant="primary" label="Ligar" disabled={!tel} onPress={() => tel && Linking.openURL(`tel:${tel.replace(/[^\d+]/g, '')}`)} />
          <Button small flex variant="secondary" label="E-mail" onPress={() => Linking.openURL(`mailto:${l.email}?subject=${encodeURIComponent('REGLA Imóveis')}`)} />
          <Button small flex variant="coral" label="WhatsApp" disabled={!tel} onPress={() => tel && Linking.openURL(waToClient(tel, waText))} />
        </View>
        {!tel && !phone.isLoading ? <Text style={[type.small, { marginTop: 6 }]}>Sem telefone cadastrado — use o e-mail.</Text> : null}
      </Card>

      <Card>
        <Row style={{ justifyContent: 'space-between', marginBottom: space.sm }}>
          <Text style={type.over}>Mensagem</Text>
          <Text style={type.small}>{relativeTime(l.created_at)}</Text>
        </Row>
        <Text style={type.body} selectable>{l.message}</Text>
        <View style={styles.meta}>
          {l.intent ? <Text style={styles.metaItem}>Interesse: <Text style={styles.metaStrong}>{INTENT_LABEL[l.intent] ?? l.intent}</Text></Text> : null}
          {l.region ? <Text style={styles.metaItem}>Região: <Text style={styles.metaStrong}>{l.region}</Text></Text> : null}
        </View>
        {l.listings ? (
          <Card style={styles.linked} onPress={() => router.push({ pathname: '/listing/[id]', params: { id: String(l.listings!.id) } })}>
            <Text style={type.mono}>{listingRef(l.listings.id)}</Text>
            <Text style={[type.h2, { flex: 1 }]} numberOfLines={1}>{l.listings.title}</Text>
            <Text style={{ color: colors.coral, fontWeight: '800' }}>›</Text>
          </Card>
        ) : null}
      </Card>

      <Card>
        <Text style={[type.over, { marginBottom: space.sm }]}>Estágio do funil</Text>
        <Segmented testIDPrefix="lead-stage" options={STAGES} value={l.stage} labels={STAGE_LABEL} onChange={(stage) => patch.mutate({ stage })} />
        <Text style={[type.over, { marginTop: space.lg, marginBottom: space.sm }]}>Prioridade</Text>
        <Segmented testIDPrefix="lead-priority" options={PRIORITIES} value={l.priority} labels={PRIORITY_LABEL} onChange={(priority) => patch.mutate({ priority })} />
        <Button
          small
          variant="ghost"
          label={l.read ? 'Marcar como não lido' : 'Marcar como lido'}
          onPress={() => patch.mutate({ read: !l.read })}
          style={{ alignSelf: 'flex-start', marginTop: space.md, paddingHorizontal: 0 }}
          testID="lead-toggle-read"
        />
        <InlineError message={patch.error ? (patch.error as Error).message : null} />
      </Card>

      <SectionHeader title={`Notas (${notes.data?.length ?? 0})`} />
      <Card>
        <TextInput
          value={note}
          onChangeText={setNote}
          placeholder="Registrar ligação, visita, combinado…"
          placeholderTextColor={colors.faint}
          multiline
          style={styles.noteInput}
          testID="note-input"
        />
        <InlineError message={add.error ? (add.error as Error).message : null} />
        <Button
          small
          label="Adicionar nota"
          disabled={!note.trim()}
          loading={add.isPending}
          onPress={() => add.mutate(note.trim())}
          style={{ alignSelf: 'flex-end', marginTop: space.sm }}
          testID="note-submit"
        />
      </Card>
      {notes.isLoading ? <Loading /> : notes.error ? <ErrorState error={notes.error} /> : (
        <View style={styles.timeline}>
          {(notes.data ?? []).map((n, i, arr) => (
            <View key={n.id} style={styles.tlItem}>
              <View style={styles.tlRail}>
                <View style={styles.tlDot} />
                {i < arr.length - 1 ? <View style={styles.tlLine} /> : null}
              </View>
              <View style={{ flex: 1, paddingBottom: space.md }}>
                <Text style={type.small}>{relativeTime(n.created_at)}{n.author_id === userId ? ' · você' : ''}</Text>
                <Text style={type.body} selectable>{n.body}</Text>
              </View>
            </View>
          ))}
          {!notes.data?.length ? <Text style={[type.small, { textAlign: 'center' }]}>Nenhuma nota ainda.</Text> : null}
        </View>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  contactRow: { flexDirection: 'row', gap: space.sm, marginTop: space.md },
  meta: { flexDirection: 'row', flexWrap: 'wrap', gap: space.md, marginTop: space.sm },
  metaItem: { fontSize: 13, color: colors.muted },
  metaStrong: { color: colors.text, fontWeight: '700' },
  linked: { flexDirection: 'row', alignItems: 'center', gap: space.sm, marginTop: space.md, padding: space.md, backgroundColor: colors.navySoft },
  noteInput: {
    minHeight: 64, borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, padding: space.md,
    fontSize: 14, color: colors.text, backgroundColor: colors.navySoft, textAlignVertical: 'top',
  },
  timeline: { paddingHorizontal: space.xs },
  tlItem: { flexDirection: 'row', gap: space.md },
  tlRail: { width: 12, alignItems: 'center' },
  tlDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: colors.coral, marginTop: 4 },
  tlLine: { flex: 1, width: 2, backgroundColor: colors.line, marginTop: 2 },
});
