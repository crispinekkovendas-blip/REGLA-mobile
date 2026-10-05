import { useState } from 'react';
import { StyleSheet, Text, TextInput, View } from 'react-native';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { APPLICATION_STATUS_LABEL, reviewApplication, type ApplicationStatus } from '@regla/shared';
import { getSupabase } from '../lib/supabase';
import { qk } from '../lib/queryClient';
import { colors, radius, space, type } from '../theme';
import { Button, Card, InlineError, Pill } from './ui';
import { APPLICATION_STATUS_TONE } from '../utils/labels';

type ReviewStatus = Extract<ApplicationStatus, 'under_review' | 'docs_requested' | 'approved' | 'rejected'>;

export const REVIEW_ACTIONS: { status: ReviewStatus; label: string; variant: 'secondary' | 'primary' | 'ok' | 'danger'; needsNote: boolean }[] = [
  { status: 'under_review', label: 'Em análise', variant: 'secondary', needsNote: false },
  { status: 'docs_requested', label: 'Pedir documentos', variant: 'secondary', needsNote: true },
  { status: 'approved', label: 'Aprovar', variant: 'ok', needsNote: false },
  { status: 'rejected', label: 'Recusar', variant: 'danger', needsNote: true },
];

/**
 * Review controls for a proposta: pick an action, optionally write a note to
 * the client, then confirm. "Pedir documentos" and "Recusar" require a note.
 */
export function ApplicationReview({ applicationId, currentStatus, currentNote, reviewerId, onDone }: {
  applicationId: number;
  currentStatus: ApplicationStatus;
  currentNote: string | null;
  reviewerId: string;
  onDone?: (status: ApplicationStatus) => void;
}) {
  const qc = useQueryClient();
  const [selected, setSelected] = useState<ReviewStatus | null>(null);
  const [note, setNote] = useState(currentNote ?? '');
  const [error, setError] = useState<string | null>(null);

  const m = useMutation({
    mutationFn: (v: { status: ReviewStatus; note: string | null }) =>
      reviewApplication(getSupabase(), applicationId, v.status, v.note, reviewerId),
    onSuccess: (_d, v) => {
      setSelected(null);
      qc.invalidateQueries({ queryKey: qk.application(applicationId) });
      qc.invalidateQueries({ queryKey: qk.applications });
      qc.invalidateQueries({ queryKey: qk.stats });
      onDone?.(v.status);
    },
    onError: (e) => setError(e instanceof Error ? e.message : String(e)),
  });

  const locked = currentStatus === 'withdrawn';
  const action = REVIEW_ACTIONS.find((a) => a.status === selected);

  const confirm = () => {
    if (!action) return;
    const trimmed = note.trim();
    if (action.needsNote && !trimmed) {
      setError(action.status === 'rejected'
        ? 'Explique o motivo da recusa para o cliente.'
        : 'Diga ao cliente quais documentos faltam.');
      return;
    }
    setError(null);
    m.mutate({ status: action.status, note: trimmed || null });
  };

  return (
    <Card testID="application-review">
      <View style={styles.head}>
        <Text style={type.over}>Análise da proposta</Text>
        <Pill label={APPLICATION_STATUS_LABEL[currentStatus]} tone={APPLICATION_STATUS_TONE[currentStatus]} testID="review-current-status" />
      </View>
      {locked ? (
        <Text style={[type.body, { color: colors.muted }]}>O cliente cancelou esta proposta.</Text>
      ) : (
        <>
          <View style={styles.grid}>
            {REVIEW_ACTIONS.map((a) => {
              const isCurrent = a.status === currentStatus;
              const isSel = a.status === selected;
              return (
                <Button
                  key={a.status}
                  testID={`review-${a.status}`}
                  label={isCurrent ? `${a.label} ✓` : a.label}
                  variant={isSel ? (a.variant === 'secondary' ? 'primary' : a.variant) : 'secondary'}
                  small
                  disabled={m.isPending}
                  onPress={() => { setSelected(isSel ? null : a.status); setError(null); }}
                  style={[styles.action, isSel && styles.actionSel]}
                />
              );
            })}
          </View>
          <Text style={[type.over, { marginTop: space.md, marginBottom: 6 }]}>
            Nota para o cliente{action?.needsNote ? ' (obrigatória)' : ' (opcional)'}
          </Text>
          <TextInput
            testID="review-note"
            value={note}
            onChangeText={setNote}
            placeholder={action?.status === 'docs_requested'
              ? 'Ex.: envie os 3 últimos holerites e comprovante de residência.'
              : 'Mensagem que o cliente verá no app'}
            placeholderTextColor={colors.faint}
            multiline
            style={styles.input}
          />
          <InlineError message={error} />
          <Button
            testID="review-confirm"
            label={action ? `Confirmar: ${action.label}` : 'Selecione uma ação'}
            variant={action?.status === 'rejected' ? 'danger' : action?.status === 'approved' ? 'ok' : 'primary'}
            disabled={!action}
            loading={m.isPending}
            onPress={confirm}
            style={{ marginTop: space.md }}
          />
        </>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  head: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: space.md },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm },
  action: { flexBasis: '47%', flexGrow: 1 },
  actionSel: { borderWidth: 2 },
  input: {
    minHeight: 72, borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, padding: space.md,
    fontSize: 14, color: colors.text, backgroundColor: colors.navySoft, textAlignVertical: 'top',
  },
});
